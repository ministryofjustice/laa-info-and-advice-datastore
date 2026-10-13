package uk.gov.justice.laa.ia.datastore.models;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** Model representing a claim from the Claim API. */
@Builder
@Getter
@Setter
public class ClaimsModel {
  private UUID claimId;
  private String status;
  private OffsetDateTime createdOn;
  private OffsetDateTime updatedOn;
}
