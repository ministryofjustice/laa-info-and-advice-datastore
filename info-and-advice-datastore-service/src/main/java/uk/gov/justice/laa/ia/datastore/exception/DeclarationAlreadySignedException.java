package uk.gov.justice.laa.ia.datastore.exception;

import java.util.UUID;

/**
 * Exception thrown when a patch to an application's declaration is attempted but the existing
 * declaration has already been signed (i.e. its {@code dateSigned} is set). A signed declaration is
 * immutable; a new declaration must be created instead of editing the existing one.
 */
public class DeclarationAlreadySignedException extends RuntimeException {
  /**
   * Creates an exception referencing the application whose declaration has already been signed.
   *
   * @param applicationId the id of the application
   */
  public DeclarationAlreadySignedException(UUID applicationId) {
    super(
        "Declaration for application %s has already been signed and cannot be edited."
            .formatted(applicationId));
  }
}
