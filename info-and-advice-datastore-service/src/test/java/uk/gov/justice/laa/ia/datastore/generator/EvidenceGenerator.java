package uk.gov.justice.laa.ia.datastore.generator;

import java.util.Map;
import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;

/** Generator for creating evidence maps for testing purposes. */
public class EvidenceGenerator {

  /** Creates a sample evidence map for testing purposes. */
  public static Map<String, Object> createEvidenceMap() {
    return Map.of(
        "payeIncomeEvidence", true,
        "otherIncomeEvidence", true,
        "housingCostsEvidence", true,
        "capitalEvidence", true);
  }

  /** Creates an {@link UpdateEvidenceCommand} with the given eTag and sample evidence data. */
  public static UpdateEvidenceCommand createUpdateEvidenceCommand(long etag) {
    return UpdateEvidenceCommand.builder()
        .eTag(etag)
        .additionalProperties(createEvidenceMap())
        .build();
  }
}
