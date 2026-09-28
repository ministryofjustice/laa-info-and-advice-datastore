package uk.gov.justice.laa.ia.datastore.exception;

/** Exception thrown when a client details patch produces an invalid client state. */
public class InvalidClientDetailsPatchException extends RuntimeException {
  public InvalidClientDetailsPatchException(String message) {
    super(message);
  }
}
