package uk.gov.justice.laa.ia.datastore.generator;

import java.time.LocalDate;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;

/** Generator for setting for an CreateClientCommand for tests. */
public class CreateClientCommandGenerator {
  /** Generator for setting for an CreateClientCommand for tests. */
  public static CreateClientCommand create(Consumer<CreateClientCommand.Builder> customizer) {
    var builder =
        CreateClientCommand.builder()
            .firstName("Joe")
            .lastName("Bloggs")
            .dateOfBirth(LocalDate.of(1990, 1, 1))
            .nationalInsuranceNumber("AC123456D");
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder.build();
  }
}
