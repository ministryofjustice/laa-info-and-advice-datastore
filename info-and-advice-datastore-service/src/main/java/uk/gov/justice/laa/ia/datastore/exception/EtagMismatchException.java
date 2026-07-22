package uk.gov.justice.laa.ia.datastore.exception;

/** Exception thrown when an ETag version mismatch is detected during an update. */
public class EtagMismatchException extends RuntimeException {
  public EtagMismatchException() {
    super("ETag version mismatch: the resource has been modified by another user");
  }
}
