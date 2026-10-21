package uk.gov.justice.laa.ia.datastore.audit;

/** The audit event taxonomy. Action names are a contract with OpenSearch/SOC; don't rename them. */
public final class AuditAction {

  /** An application was created in draft. */
  public static final String APPLICATION_STARTED = "application.started";

  /** An application's metadata and/or linked client, declaration and evidence were changed. */
  public static final String APPLICATION_UPDATED = "application.updated";

  /** An application's client details were updated. */
  public static final String APPLICATION_CLIENT_DETAILS_UPDATED =
      "application.clientDetailsUpdated";

  /** An application's means data was updated. */
  public static final String APPLICATION_MEANS_UPDATED = "application.meansUpdated";

  /** An application's scoping data was updated. */
  public static final String APPLICATION_SCOPING_UPDATED = "application.scopingUpdated";

  /** An application's client declaration was updated. */
  public static final String APPLICATION_DECLARATION_UPDATED = "application.declarationUpdated";

  /** An application's evidence was updated. */
  public static final String APPLICATION_EVIDENCE_UPDATED = "application.evidenceUpdated";

  /** An application moved from draft to completed. */
  public static final String APPLICATION_SUBMITTED = "application.submitted";

  /** A single application was read. */
  public static final String APPLICATION_VIEWED = "application.viewed";

  /** A list of applications was read. */
  public static final String APPLICATION_LISTED = "application.listed";

  /** The scheduled retention resolver set an application's data retention date. */
  public static final String APPLICATION_RETENTION_DATE_SET = "application.retentionDateSet";

  /** The scheduled deletion task removed an application's PII. */
  public static final String APPLICATION_DATA_DELETED = "application.dataDeleted";

  /** A caller exceeded its request rate limit. */
  public static final String RATE_LIMIT_EXCEEDED = "security.rateLimitExceeded";

  /** A token was missing, malformed, expired or otherwise failed verification. */
  public static final String TOKEN_VERIFICATION_FAILED = "security.tokenVerificationFailed";

  /** A verified caller lacked the claims or scopes needed for the request. */
  public static final String CLAIMS_AUTHORISATION_FAILED = "security.claimsAuthorisationFailed";

  private AuditAction() {}
}
