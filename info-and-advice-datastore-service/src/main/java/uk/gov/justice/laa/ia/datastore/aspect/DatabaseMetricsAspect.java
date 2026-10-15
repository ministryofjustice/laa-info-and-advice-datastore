package uk.gov.justice.laa.ia.datastore.aspect;

import io.sentry.Sentry;
import io.sentry.SentryAttribute;
import io.sentry.SentryAttributes;
import io.sentry.metrics.MetricsUnit;
import io.sentry.metrics.SentryMetricsParameters;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Records a Sentry distribution metric for the duration of every Spring Data repository call. */
@Aspect
@Component
public class DatabaseMetricsAspect {

  private static final String METRIC_NAME = "db.query.duration";

  @Around("execution(* uk.gov.justice.laa.ia.datastore.repository..*(..))")
  public Object recordQueryDuration(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.nanoTime();
    try {
      return joinPoint.proceed();
    } finally {
      double durationMs = (System.nanoTime() - start) / 1_000_000.0;
      Sentry.metrics()
          .distribution(
              METRIC_NAME,
              durationMs,
              MetricsUnit.Duration.MILLISECOND,
              SentryMetricsParameters.create(
                  SentryAttributes.of(
                      SentryAttribute.stringAttribute(
                          "repository",
                          joinPoint.getSignature().getDeclaringType().getSimpleName()),
                      SentryAttribute.stringAttribute(
                          "method", joinPoint.getSignature().getName()))));
    }
  }
}
