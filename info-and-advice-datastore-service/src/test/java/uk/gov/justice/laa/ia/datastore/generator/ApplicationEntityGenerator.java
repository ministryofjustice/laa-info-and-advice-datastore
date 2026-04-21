package uk.gov.justice.laa.ia.datastore.generator;

import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ClientCaseDetailsStatus;
import uk.gov.justice.laa.ia.datastore.model.OverallApplicationStatus;

/** Generator for setting for an ApplicationEntity for tests. */
public class ApplicationEntityGenerator {

  /**
   * Creates an Application entity with an Id, used for tests that will not persist to a real data
   * source that will generate the Id.
   */
  public static ApplicationEntity createWithId(
      UUID individualId, Consumer<ApplicationEntity.ApplicationEntityBuilder> customizer) {
    return createApplication(individualId, customizer).id(UUID.randomUUID()).build();
  }

  /**
   * Creates an Application entity with an Id, used for tests that will persist to a real data store
   * that will generate the Id.
   */
  public static ApplicationEntity createWithoutId(
      UUID individualId, Consumer<ApplicationEntity.ApplicationEntityBuilder> customizer) {
    return createApplication(individualId, customizer).build();
  }

  private static ApplicationEntity.ApplicationEntityBuilder createApplication(
      UUID individualId, Consumer<ApplicationEntity.ApplicationEntityBuilder> customizer) {
    var builder =
        ApplicationEntity.builder()
            .individualLegalAidNumber(individualId)
            .providerFirmId(UUID.randomUUID())
            .providerOfficeId(UUID.randomUUID())
            .eligibilityResultId(UUID.randomUUID())
            .clientCaseDetailsStatus(ClientCaseDetailsStatus.DRAFT)
            .meansAssessmentStatusId(UUID.randomUUID())
            .evidenceStatusId(UUID.randomUUID())
            .clientDeclarationStatusId(UUID.randomUUID())
            .overallApplicationStatus(OverallApplicationStatus.DRAFT)
            .uniqueFileNumber(UUID.randomUUID())
            .createdBy("Joe Bloggs")
            .modifiedBy("Joe Bloggs");
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder;
  }
}
