package uk.gov.justice.laa.ia.datastore.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes audit events as structured log entries to the {@value #LOGGER_NAME} logger. Container logs
 * are shipped to OpenSearch (30 day retention) and exported by the SOC integration (3 month
 * retention), so entries carry everything needed to reconstruct who did what without the database.
 *
 * <p>Every entry has {@code event.category} (domain or security), {@code event.action}, {@code
 * event.outcome}, {@code user.id} (actor) and {@code resource.type}/{@code resource.id}. {@code
 * correlationId}, {@code trace.id} and {@code span.id} come from the MDC populated by the request
 * filters. Entries are always written at INFO so they survive production log-level tuning. Never
 * pass PII (names, dates of birth, etc.) - IDs only. Firm and office codes ({@code
 * labels.providerFirmCode}/{@code labels.providerOfficeCode}) and the Entra tenant ({@code
 * labels.tenantId}) are added when known.
 */
@Component
public class AuditLogger {

  /** Logger name; also the hook for routing or retention rules on the log pipeline. */
  public static final String LOGGER_NAME = "audit";

  /** Request attribute set once a security event has been audited for the request. */
  public static final String SECURITY_AUDITED_ATTRIBUTE = AuditLogger.class.getName() + ".security";

  public static final String ANONYMOUS_ACTOR = "anonymous";

  private static final Logger AUDIT = LoggerFactory.getLogger(LOGGER_NAME);

  /** Outcome of an audited action. */
  public enum Outcome {
    SUCCESS,
    FAILURE
  }

  /**
   * Audits a domain event. If a transaction is active, the entry is written when it completes so
   * the outcome reflects whether the change was actually committed.
   *
   * @param action one of {@link AuditAction}
   * @param actorId ID of the user or system process that performed the action
   * @param resourceType type of the affected resource, e.g. {@code application}
   * @param resourceId ID of the affected resource
   * @param providerFirmCode firm the resource belongs to, or null if unknown
   * @param providerOfficeCode office the resource belongs to, or null if unknown
   */
  public void domain(
      String action,
      String actorId,
      String resourceType,
      String resourceId,
      String providerFirmCode,
      String providerOfficeCode) {
    final String tenantId = currentTenant();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              write(
                  "domain",
                  action,
                  status == STATUS_COMMITTED ? Outcome.SUCCESS : Outcome.FAILURE,
                  actorId,
                  resourceType,
                  resourceId,
                  null,
                  providerFirmCode,
                  providerOfficeCode,
                  tenantId);
            }
          });
    } else {
      write(
          "domain",
          action,
          Outcome.SUCCESS,
          actorId,
          resourceType,
          resourceId,
          null,
          providerFirmCode,
          providerOfficeCode,
          tenantId);
    }
  }

  /**
   * Audits a security event. Security events are always failures from the caller's point of view.
   *
   * @param action one of {@link AuditAction}
   * @param actorId ID of the caller, or {@link #ANONYMOUS_ACTOR} if it couldn't be established
   * @param resourceType type of the resource the caller tried to reach
   * @param resourceId ID of the resource the caller tried to reach
   * @param reason a short, PII-free explanation
   */
  public void security(
      String action, String actorId, String resourceType, String resourceId, String reason) {
    security(action, actorId, resourceType, resourceId, reason, null, null);
  }

  /**
   * Audits a security event for a caller whose firm and/or office is known.
   *
   * @param action one of {@link AuditAction}
   * @param actorId ID of the caller, or {@link #ANONYMOUS_ACTOR} if it couldn't be established
   * @param resourceType type of the resource the caller tried to reach
   * @param resourceId ID of the resource the caller tried to reach
   * @param reason a short, PII-free explanation
   * @param providerFirmCode the caller's firm, or null if unknown
   * @param providerOfficeCode the office involved, or null if not applicable
   */
  public void security(
      String action,
      String actorId,
      String resourceType,
      String resourceId,
      String reason,
      String providerFirmCode,
      String providerOfficeCode) {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      attrs.getRequest().setAttribute(SECURITY_AUDITED_ATTRIBUTE, Boolean.TRUE);
    }
    write(
        "security",
        action,
        Outcome.FAILURE,
        actorId,
        resourceType,
        resourceId,
        reason,
        providerFirmCode,
        providerOfficeCode,
        currentTenant());
  }

  /**
   * Whether a security event was already audited for the request.
   *
   * @param request the current request
   * @return true if so
   */
  public static boolean isSecurityAudited(HttpServletRequest request) {
    return request.getAttribute(SECURITY_AUDITED_ATTRIBUTE) != null;
  }

  /**
   * The ID of the authenticated caller (the {@code oid} claim), if any.
   *
   * @return the actor ID or {@link #ANONYMOUS_ACTOR}
   */
  public static String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwt) {
      String oid = jwt.getToken().getClaimAsString("oid");
      if (oid != null && !oid.isBlank()) {
        return oid;
      }
    }
    return ANONYMOUS_ACTOR;
  }

  /**
   * The Entra tenant ({@code tid} claim) of the authenticated caller, if any.
   *
   * @return the tenant ID or null
   */
  public static String currentTenant() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwt) {
      return jwt.getToken().getClaimAsString("tid");
    }
    return null;
  }

  private void write(
      String category,
      String action,
      Outcome outcome,
      String actorId,
      String resourceType,
      String resourceId,
      String reason,
      String providerFirmCode,
      String providerOfficeCode,
      String tenantId) {
    var event =
        AUDIT
            .atInfo()
            .addKeyValue("event.category", category)
            .addKeyValue("event.action", action)
            .addKeyValue("event.outcome", outcome.name().toLowerCase())
            .addKeyValue("user.id", actorId == null ? ANONYMOUS_ACTOR : actorId)
            .addKeyValue("resource.type", resourceType)
            .addKeyValue("resource.id", resourceId);
    if (reason != null) {
      event = event.addKeyValue("event.reason", reason);
    }
    if (providerFirmCode != null) {
      event = event.addKeyValue("labels.providerFirmCode", providerFirmCode);
    }
    if (providerOfficeCode != null) {
      event = event.addKeyValue("labels.providerOfficeCode", providerOfficeCode);
    }
    if (tenantId != null) {
      event = event.addKeyValue("labels.tenantId", tenantId);
    }
    event.log("audit.{}", action);
  }
}
