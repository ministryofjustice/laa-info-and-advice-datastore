package uk.gov.justice.laa.ia.datastore.generator;

import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;

/** Generator for creating evidence commands for testing purposes. */
public class EvidenceGenerator {

  /** Creates an {@link UpdateEvidenceCommand} with the given eTag and sample evidence data. */
  public static UpdateEvidenceCommand createUpdateEvidenceCommand(long etag) {
    return UpdateEvidenceCommand.builder()
        .eTag(etag)
        .evidenceExemptionCode("EXEMPT_01")
        .evidenceExemptionReason("Client is exempt from providing evidence")
        .build();
  }
}
