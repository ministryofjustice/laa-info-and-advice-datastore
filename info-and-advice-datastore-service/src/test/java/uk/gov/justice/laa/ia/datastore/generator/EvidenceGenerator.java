package uk.gov.justice.laa.ia.datastore.generator;

import java.util.Map;

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
}
