package uk.gov.justice.laa.ia.datastore.generator;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.model.EvidenceStatus;

/** Generator for setting for an EvidenceEntity for tests. */
public class EvidenceEntityGenerator {

  /**
   * Creates an Evidence entity with an Id, used for tests that will not persist to a real data
   * source that will generate the Id.
   */
  public static EvidenceEntity createWithId(
      UUID applicationId, Consumer<EvidenceEntity.EvidenceEntityBuilder> customizer) {
    return createEvidence(applicationId, customizer)
        .id(UUID.randomUUID())
        .createdAt(Instant.now())
        .modifiedAt(Instant.now())
        .build();
  }

  /**
   * Creates an Evidence entity with an Id, used for tests that will persist to a real data store
   * that will generate the Id.
   */
  public static EvidenceEntity createWithoutId(
      UUID applicationId, Consumer<EvidenceEntity.EvidenceEntityBuilder> customizer) {
    return createEvidence(applicationId, customizer).build();
  }

  private static EvidenceEntity.EvidenceEntityBuilder createEvidence(
      UUID applicationId, Consumer<EvidenceEntity.EvidenceEntityBuilder> customizer) {
    var builder =
        EvidenceEntity.builder()
            .referenceNumber(applicationId)
            .evidenceStatus(EvidenceStatus.DRAFT)
            .payeIncomeEvidence(false)
            .otherIncomeEvidence(false)
            .housingCostsEvidence(false)
            .capitalEvidence(false)
            .createdBy("Joe Bloggs")
            .modifiedBy("James Bloggs");
    if (customizer != null) {
      customizer.accept(builder);
    }
    return builder;
  }
}
