package uk.gov.justice.laa.ia.datastore.generator;

import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.model.CategoryOfLaw;
import uk.gov.justice.laa.ia.datastore.model.ProviderContractType;
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
            .providerOfficeCode(UUID.randomUUID().toString())
            .providerContractType(ProviderContractType.FACE_TO_FACE)
            .categoryOfLaw(CategoryOfLaw.FAMILY);

    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder.build();
  }
}
