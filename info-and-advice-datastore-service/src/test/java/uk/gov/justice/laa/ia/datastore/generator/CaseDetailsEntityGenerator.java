package uk.gov.justice.laa.ia.datastore.generator;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.entity.CaseDetailsEntity;

/** Generator for setting for an CaseDetailsEntity for tests. */
public class CaseDetailsEntityGenerator {
  /**
   * Creates an CaseDetails entity with an Id, used for tests that will not persist to a real data
   * source that will generate the Id.
   */
  public static CaseDetailsEntity createWithId(
      Consumer<CaseDetailsEntity.CaseDetailsEntityBuilder> customizer) {
    return createCaseDetails(customizer)
        .id(UUID.randomUUID())
        .createdAt(Instant.now())
        .createdBy("Joe Bloggs")
        .modifiedAt(Instant.now())
        .modifiedBy("Joe Bloggs")
        .build();
  }

  /**
   * Creates an CaseDetails entity with an Id, used for tests that will persist to a real data store
   * that will generate the Id.
   */
  public static CaseDetailsEntity createWithoutId(
      Consumer<CaseDetailsEntity.CaseDetailsEntityBuilder> customizer) {
    return createCaseDetails(customizer).build();
  }

  private static CaseDetailsEntity.CaseDetailsEntityBuilder createCaseDetails(
      Consumer<CaseDetailsEntity.CaseDetailsEntityBuilder> customizer) {
    var builder =
        CaseDetailsEntity.builder()
            .requireEcf(false)
            .hasPreviousLegalAid(false)
            .hasSixMonthsLegalHelp(false)
            .meansAssessmentRequired(false)
            .typeNonMeansTested(false)
            .createdBy("Joe Bloggs")
            .modifiedBy("Joe Bloggs");
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder;
  }
}
