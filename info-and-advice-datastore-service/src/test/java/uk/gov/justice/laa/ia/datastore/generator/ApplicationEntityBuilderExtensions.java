package uk.gov.justice.laa.ia.datastore.generator;

import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;

/** Extension methods for ApplicationEntityBuilder. */
public class ApplicationEntityBuilderExtensions {
  /**
   * Extension method to set default client details on an ApplicationEntityBuilder.
   *
   * @param builder the ApplicationEntityBuilder to set default client details on
   * @return the modified ApplicationEntityBuilder with default client details set
   */
  public static ApplicationEntity.ApplicationEntityBuilder withDefaultClientDetails(
      ApplicationEntity.ApplicationEntityBuilder builder) {
    return builder.clientDetails(
        ClientDetailsEntityGenerator.createWithoutId(
            clientBuilder -> {
              clientBuilder.address(AddressEntityGenerator.createWithoutId(null));
            }));
  }
}
