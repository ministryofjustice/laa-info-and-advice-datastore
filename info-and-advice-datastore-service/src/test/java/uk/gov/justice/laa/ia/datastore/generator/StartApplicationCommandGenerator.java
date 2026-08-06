package uk.gov.justice.laa.ia.datastore.generator;

import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;

/** Generator for setting for an StartApplicationCommand for tests. */
public class StartApplicationCommandGenerator {
  /** Creates a default StartApplicationCommand. */
  public static StartApplicationCommand create(
      Consumer<StartApplicationCommand.Builder> customizer) {
    StartApplicationCommand.Builder builder =
        StartApplicationCommand.builder()
            .client(CreateClientCommandGenerator.create(null))
            .applicationType(StartApplicationCommand.ApplicationTypeEnum.RCW)
            .providerOfficeId(UUID.randomUUID().toString());

    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder.build();
  }
}
