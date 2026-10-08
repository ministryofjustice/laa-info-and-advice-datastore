package uk.gov.justice.laa.ia.datastore.exception;

/**
 * Exception thrown when a create/update/patch request's payload is identical to one already
 * recorded in the event history for the application, indicating a duplicate submission.
 */
public class DuplicatePayloadException extends RuntimeException {
  /** Creates an exception indicating a duplicate request payload was detected. */
  public DuplicatePayloadException() {
    super("Application already exists.");
  }
}
