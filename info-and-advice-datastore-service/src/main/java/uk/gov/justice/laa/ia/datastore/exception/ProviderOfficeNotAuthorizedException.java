package uk.gov.justice.laa.ia.datastore.exception;

/**
 * Exception thrown when a request references a provider office code the user is not authorized for.
 */
public class ProviderOfficeNotAuthorizedException extends RuntimeException {
  /**
   * Creates an exception referencing the unauthorized provider office code.
   *
   * @param providerOfficeCode the provider office code the user is not authorized for
   */
  public ProviderOfficeNotAuthorizedException(String providerOfficeCode) {
    super("Provider office code %s is not authorized for this user.".formatted(providerOfficeCode));
  }
}
