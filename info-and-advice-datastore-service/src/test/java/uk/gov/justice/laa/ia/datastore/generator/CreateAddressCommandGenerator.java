package uk.gov.justice.laa.ia.datastore.generator;

import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.model.CreateAddressCommand;

/** Generator for setting for an CreateAddressCommandGenerator for tests. */
public class CreateAddressCommandGenerator {
  /** Create a default create address command. */
  public static CreateAddressCommand create(Consumer<CreateAddressCommand.Builder> customizer) {
    var builder =
        CreateAddressCommand.builder()
            .line1("Address line 1")
            .line2("Address line 2")
            .city("City")
            .country("Country")
            .postCode("SW11 1A");

    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder.build();
  }
}
