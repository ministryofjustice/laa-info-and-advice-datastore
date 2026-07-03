package uk.gov.justice.laa.ia.datastore.generator;

import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;

/** Generator for setting for an StartCaseCommand for tests. */
public class StartCaseCommandGenerator {
  /** Creates a default StartCaseCommand. */
  public static StartCaseCommand create(Consumer<StartCaseCommand.Builder> customizer) {
    StartCaseCommand.Builder builder =
        StartCaseCommand.builder()
            .client(CreateClientCommandGenerator.create(null))
            .applicationType(StartCaseCommand.ApplicationTypeEnum.RCW)
            .providerOfficeId(UUID.randomUUID());

    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder.build();
  }
}
