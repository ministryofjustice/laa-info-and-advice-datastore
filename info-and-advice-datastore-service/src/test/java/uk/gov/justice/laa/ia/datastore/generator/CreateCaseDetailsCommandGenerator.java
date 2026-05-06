package uk.gov.justice.laa.ia.datastore.generator;

import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.model.CreateCaseDetailsCommand;

/** Generator for setting for an CreateCaseDetailsCommand for tests. */
public class CreateCaseDetailsCommandGenerator {

  /** Creates a default create case details command. */
  public static CreateCaseDetailsCommand create(
      Consumer<CreateCaseDetailsCommand.Builder> customizer) {
    CreateCaseDetailsCommand.Builder builder =
        CreateCaseDetailsCommand.builder()
            .hasLegalHelpLastSixMonths(false)
            .hasPreviousLegalAid(false)
            .isTypeNonMeansTested(false)
            .meansAssessmentRequired(false)
            .requiresEcf(false);

    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder.build();
  }
}
