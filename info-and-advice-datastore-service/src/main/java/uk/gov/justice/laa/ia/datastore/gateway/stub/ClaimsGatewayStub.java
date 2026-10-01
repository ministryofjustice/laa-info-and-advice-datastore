package uk.gov.justice.laa.ia.datastore.gateway.stub;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;
import uk.gov.justice.laa.ia.datastore.models.ClaimsModel;

/** Stub implementation of the ClaimsGateway interface for testing purposes. */
@Component
public class ClaimsGatewayStub implements ClaimsGateway {

  @Override
  public ApplicationClaimResponse getClaims(String officeCode, String ufn) {
    OffsetDateTime now = java.time.OffsetDateTime.now();
    return ApplicationClaimResponse.builder()
        .claims(
            List.of(
                ClaimsModel.builder()
                    .claimId(UUID.randomUUID())
                    .status("APPROVED")
                    .createdOn(now)
                    .updatedOn(now)
                    .build()))
        .build();
  }
}
