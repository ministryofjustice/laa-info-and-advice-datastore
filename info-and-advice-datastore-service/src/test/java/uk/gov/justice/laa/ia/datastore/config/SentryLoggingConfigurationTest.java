package uk.gov.justice.laa.ia.datastore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.sentry.Sentry;
import io.sentry.SentryLogEvent;
import io.sentry.SentryMetricsEvent;
import io.sentry.SentryOptions;
import io.sentry.metrics.MetricsUnit;
import io.sentry.transport.ITransport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;

class SentryLoggingConfigurationTest {

  private final SentryLoggingConfiguration configuration = new SentryLoggingConfiguration();
  private final List<SentryLogEvent> logs = new ArrayList<>();
  private final List<SentryMetricsEvent> metrics = new ArrayList<>();
  private final AtomicInteger errorEvents = new AtomicInteger();
  private Appender<ILoggingEvent> existingAppender;

  @BeforeEach
  void setUp() {
    Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    existingAppender = root.getAppender("SENTRY_STRUCTURED_LOGS");
    if (existingAppender != null) {
      root.detachAppender(existingAppender);
    }
    SentryOptions options = new SentryOptions();
    options.setDsn("https://public@example.invalid/1");
    options.setRelease("datastore@test-sha");
    options.getIntegrations().clear();
    options.setTransportFactory((ignoredOptions, requestDetails) -> mock(ITransport.class));
    options.getLogs().setEnabled(true);
    options
        .getLogs()
        .setBeforeSend(
            event -> {
              logs.add(new SentryPiiRedactor().execute(event));
              return null;
            });
    options
        .getMetrics()
        .setBeforeSend(
            (metric, hint) -> {
              metrics.add(metric);
              return null;
            });
    options.setBeforeSend(
        (event, hint) -> {
          errorEvents.incrementAndGet();
          return null;
        });
    Sentry.init(options);
    configuration.onApplicationEvent(mock(ContextRefreshedEvent.class));
  }

  @AfterEach
  void tearDown() {
    configuration.destroy();
    Sentry.close();
    if (existingAppender != null) {
      Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
      root.addAppender(existingAppender);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "firstName", "lastName", "dateOfBirth", "niNumber", "nationalInsuranceNumber",
        "addressLine1", "addressLine2", "addressLine3", "addressLine4", "townOrCity",
        "postCode", "county", "clientFirstName", "clientLastName"
      })
  void capturesAndRedactsRealLogbackMessagesAndTheirParameters(String field) {
    LoggerFactory.getLogger(getClass()).info(field + "=\"{}\"", "Sensitive Name");

    assertThat(logs).hasSize(1);
    SentryLogEvent event = logs.getFirst();
    assertThat(event.getBody()).doesNotContain("Sensitive Name").contains("[REDACTED]");
    assertThat(event.getAttributes().get("sentry.message.parameter.0").getValue())
        .isEqualTo("[REDACTED]");
    assertThat(event.getAttributes().get("sentry.release").getValue())
        .isEqualTo("datastore@test-sha");
  }

  @Test
  void doesNotCreateErrorEventsOrBreadcrumbsFromLogback() {
    LoggerFactory.getLogger(getClass()).error("A safe test error");

    assertThat(logs).hasSize(1);
    assertThat(errorEvents).hasValue(0);
    assertThat(Sentry.getCurrentScopes().getScope().getBreadcrumbs()).isEmpty();
  }

  @Test
  void capturesMetricsWithReleaseAndMillisecondUnit() {
    Sentry.metrics().distribution("db.query.duration", 12.0, MetricsUnit.Duration.MILLISECOND);

    assertThat(metrics).hasSize(1);
    assertThat(metrics.getFirst().getName()).isEqualTo("db.query.duration");
    assertThat(metrics.getFirst().getUnit()).isEqualTo("millisecond");
    assertThat(metrics.getFirst().getAttributes().get("sentry.release").getValue())
        .isEqualTo("datastore@test-sha");
  }
}
