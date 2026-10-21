package uk.gov.justice.laa.ia.datastore.audit;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Unit tests for {@link AuditLogger}. */
class AuditLoggerTest {

  private final AuditLogger auditLogger = new AuditLogger();
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
  private final Logger logger = (Logger) LoggerFactory.getLogger(AuditLogger.LOGGER_NAME);

  @BeforeEach
  void attach() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detach() {
    logger.detachAppender(appender);
    SecurityContextHolder.clearContext();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  private Map<String, Object> onlyEvent() {
    assertThat(appender.list).hasSize(1);
    return appender.list.get(0).getKeyValuePairs().stream()
        .collect(Collectors.toMap(kv -> kv.key, kv -> kv.value));
  }

  @Test
  void read_shouldLogDetailsAndOmitNullResourceId() {
    auditLogger.read(
        AuditAction.APPLICATION_LISTED,
        "actor-1",
        "application",
        null,
        "firm-1",
        null,
        Map.of("page", 2));

    assertThat(onlyEvent())
        .containsEntry("event.category", "domain")
        .containsEntry("event.action", "application.listed")
        .containsEntry("labels.page", 2)
        .doesNotContainKey("resource.id");
  }

  @Test
  void domain_shouldLogImmediately_whenNoTransaction() {
    auditLogger.domain(
        AuditAction.APPLICATION_STARTED, "actor-1", "application", "app-1", "firm-1", "office-1");

    assertThat(onlyEvent())
        .containsEntry("event.category", "domain")
        .containsEntry("event.action", "application.started")
        .containsEntry("event.outcome", "success")
        .containsEntry("user.id", "actor-1")
        .containsEntry("resource.type", "application")
        .containsEntry("resource.id", "app-1")
        .containsEntry("labels.providerFirmCode", "firm-1")
        .containsEntry("labels.providerOfficeCode", "office-1")
        .doesNotContainKey("labels.tenantId");
    assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
  }

  @Test
  void domain_shouldLogFailureOnlyOnceTransactionRollsBack() {
    TransactionSynchronizationManager.initSynchronization();

    auditLogger.domain(
        AuditAction.APPLICATION_SUBMITTED, "actor-1", "application", "app-1", "firm-1", "office-1");
    assertThat(appender.list).isEmpty();

    TransactionSynchronizationManager.getSynchronizations()
        .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    assertThat(onlyEvent()).containsEntry("event.outcome", "failure");
  }

  @Test
  void domain_shouldLogSuccess_whenTransactionCommits() {
    TransactionSynchronizationManager.initSynchronization();

    auditLogger.domain(
        AuditAction.APPLICATION_SUBMITTED, "actor-1", "application", "app-1", "firm-1", "office-1");
    TransactionSynchronizationManager.getSynchronizations()
        .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

    assertThat(onlyEvent()).containsEntry("event.outcome", "success");
  }

  @Test
  void security_shouldLogFailureWithReason() {
    auditLogger.security(
        AuditAction.RATE_LIMIT_EXCEEDED, null, "endpoint", "GET /api/v0/applications", "too fast");

    assertThat(onlyEvent())
        .containsEntry("event.category", "security")
        .containsEntry("event.action", "security.rateLimitExceeded")
        .containsEntry("event.outcome", "failure")
        .containsEntry("user.id", "anonymous")
        .containsEntry("event.reason", "too fast");
  }

  @Test
  void security_shouldIncludeTenantAndFirmContext_whenKnown() {
    Jwt jwt =
        Jwt.withTokenValue("t")
            .header("alg", "none")
            .claim("oid", "user-1")
            .claim("tid", "tenant-1")
            .build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

    auditLogger.security(
        AuditAction.CLAIMS_AUTHORISATION_FAILED,
        AuditLogger.currentActor(),
        "provider_office",
        "office-9",
        "not authorised",
        "firm-1",
        "office-9");

    assertThat(onlyEvent())
        .containsEntry("user.id", "user-1")
        .containsEntry("labels.tenantId", "tenant-1")
        .containsEntry("labels.providerFirmCode", "firm-1")
        .containsEntry("labels.providerOfficeCode", "office-9");
  }

  @Test
  void currentActor_shouldBeAnonymous_whenUnauthenticated() {
    assertThat(AuditLogger.currentActor()).isEqualTo("anonymous");
  }
}
