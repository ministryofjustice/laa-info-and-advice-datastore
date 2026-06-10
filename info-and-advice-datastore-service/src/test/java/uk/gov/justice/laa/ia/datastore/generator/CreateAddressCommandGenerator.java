package uk.gov.justice.laa.ia.datastore.generator;

import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.model.CreateAddressCommand;

/** Generator for setting for an CreateAddressCommandGenerator for tests. */
public class CreateAddressCommandGenerator {
  /** Create a default create address command. */
  public static CreateAddressCommand create(Consumer<CreateAddressCommand.Builder> customizer) {
    var builder =
        CreateAddressCommand.builder()
            .addressLine1("Address line 1")
            .addressLine2("Address line 2")
            .addressLine3("Address line 3")
            .addressLine4("Address line 4")
            .townOrCity("City")
            .county("County")
            .country("GB")
            .postCode("SW11 1A");

    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder.build();
  }
}
