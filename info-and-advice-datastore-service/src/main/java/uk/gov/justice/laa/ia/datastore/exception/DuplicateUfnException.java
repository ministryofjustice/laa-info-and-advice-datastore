package uk.gov.justice.laa.ia.datastore.exception;

/**
 * Exception thrown when an application's UFN is not unique among applications sharing the same
 * provider office code.
 */
public class DuplicateUfnException extends RuntimeException {
  /**
   * Creates an exception referencing the duplicate UFN and provider office code.
   *
   * @param ufn the UFN that already exists for the provider office code
   * @param providerOfficeCode the provider office code the UFN must be unique within
   */
  public DuplicateUfnException(String ufn, String providerOfficeCode) {
    super("UFN %s already exists for provider office code %s.".formatted(ufn, providerOfficeCode));
  }
}
