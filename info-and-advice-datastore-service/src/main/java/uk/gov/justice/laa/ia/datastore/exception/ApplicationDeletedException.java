package uk.gov.justice.laa.ia.datastore.exception;

import java.util.UUID;

/** Exception thrown when an operation is attempted on an application that has been deleted. */
public class ApplicationDeletedException extends RuntimeException {
  /**
   * Creates an exception referencing the deleted application.
   *
   * @param applicationId the ID of the deleted application
   */
  public ApplicationDeletedException(UUID applicationId) {
    super("Application %s has been deleted due to retention policies.".formatted(applicationId));
  }
}
