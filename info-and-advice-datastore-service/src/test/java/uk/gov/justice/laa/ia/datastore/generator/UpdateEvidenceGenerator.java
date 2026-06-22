package uk.gov.justice.laa.ia.datastore.generator;

import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;

/** Generator for setting values for an UpdateEvidenceCommand for tests. */
public class UpdateEvidenceGenerator {
  /** Creates an UpdateEvidenceCommand with default values. */
  public static UpdateEvidenceCommand createUpdateEvidenceCommand(
      Consumer<UpdateEvidenceCommand.Builder> customizer) {
    UpdateEvidenceCommand.Builder builder =
        UpdateEvidenceCommand.builder()
            .payeIncomeEvidence(false)
            .otherIncomeEvidence(false)
            .housingCostsEvidence(false)
            .capitalEvidence(false);
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder.build();
  }
}
