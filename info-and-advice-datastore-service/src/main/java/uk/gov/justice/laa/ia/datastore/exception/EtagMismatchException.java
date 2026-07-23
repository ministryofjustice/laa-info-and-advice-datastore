package uk.gov.justice.laa.ia.datastore.exception;

/** Exception thrown when an ETag version mismatch is detected during an update. */
public class EtagMismatchException extends RuntimeException {
  /**
   * Creates an exception with the supplied and expected eTag values included in the message.
   *
   * @param supplied the eTag value provided by the client
   * @param expected the current eTag value of the entity
   */
  public EtagMismatchException(long supplied, long expected) {
    super(
        "ETag mismatch: supplied %d, expected %d. The resource has been modified by another user."
            .formatted(supplied, expected));
  }
}
