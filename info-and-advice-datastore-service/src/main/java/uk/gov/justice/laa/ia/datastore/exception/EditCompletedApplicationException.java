package uk.gov.justice.laa.ia.datastore.exception;

/** Exception thrown when an edit is attempted on a completed application. */
public class EditCompletedApplicationException extends RuntimeException {
  public EditCompletedApplicationException() {
    super("Completed applications cannot be edited.");
  }
}
