package uk.gov.justice.laa.ia.datastore.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.sentry.Sentry;
import io.sentry.metrics.IMetricsApi;
import io.sentry.metrics.MetricsUnit;
import io.sentry.metrics.SentryMetricsParameters;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class DatabaseMetricsAspectTest {

  private final DatabaseMetricsAspect sut = new DatabaseMetricsAspect();
  private final ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
  private final Signature signature = mock(Signature.class);
  private final IMetricsApi metrics = mock(IMetricsApi.class);

  @Test
  void recordsRepositoryDurationAndReturnsResult() throws Throwable {
    Object result = new Object();
    configureRepositoryCall("findById");
    when(joinPoint.proceed()).thenReturn(result);

    try (MockedStatic<Sentry> sentry = mockStatic(Sentry.class)) {
      sentry.when(Sentry::metrics).thenReturn(metrics);

      assertThat(sut.recordQueryDuration(joinPoint)).isSameAs(result);
    }

    verifyDurationMetric("findById");
  }

  @Test
  void recordsRepositoryDurationAndRethrowsFailure() throws Throwable {
    RuntimeException failure = new RuntimeException("repository failed");
    configureRepositoryCall("save");
    when(joinPoint.proceed()).thenThrow(failure);

    try (MockedStatic<Sentry> sentry = mockStatic(Sentry.class)) {
      sentry.when(Sentry::metrics).thenReturn(metrics);

      assertThatThrownBy(() -> sut.recordQueryDuration(joinPoint)).isSameAs(failure);
    }

    verifyDurationMetric("save");
  }

  private void configureRepositoryCall(String methodName) {
    when(joinPoint.getSignature()).thenReturn(signature);
    when(signature.getDeclaringType()).thenReturn(TestRepository.class);
    when(signature.getName()).thenReturn(methodName);
  }

  private void verifyDurationMetric(String methodName) {
    ArgumentCaptor<Double> duration = ArgumentCaptor.forClass(Double.class);
    ArgumentCaptor<SentryMetricsParameters> parameters =
        ArgumentCaptor.forClass(SentryMetricsParameters.class);

    verify(metrics)
        .distribution(
            eq("db.query.duration"),
            duration.capture(),
            eq(MetricsUnit.Duration.MILLISECOND),
            parameters.capture());

    assertThat(duration.getValue()).isGreaterThanOrEqualTo(0.0);
    assertThat(parameters.getValue().getAttributes().getAttributes())
        .containsKeys("repository", "method");
    assertThat(parameters.getValue().getAttributes().getAttributes().get("repository").getValue())
        .isEqualTo("TestRepository");
    assertThat(parameters.getValue().getAttributes().getAttributes().get("method").getValue())
        .isEqualTo(methodName);
  }

  private static class TestRepository {}
}
