package uk.gov.justice.laa.ia.datastore.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.ia.datastore.gateway.stub.ClaimsGatewayStub;

class ClaimsGatewayStubTests {

  private final ClaimsGatewayStub stub = new ClaimsGatewayStub();

  @Test
  void fixtureWithNoClaims_returnsEmptyList() {
    assertTrue(
        stub.getClaims(ClaimsGatewayStub.FIXTURE_OFFICE_CODE, ClaimsGatewayStub.NO_CLAIMS_UFN)
            .getClaims()
            .isEmpty());
  }

  @Test
  void fixtureWithMultipleClaims_returnsRecentClaimFirst() {
    final var claims =
        stub.getClaims(ClaimsGatewayStub.FIXTURE_OFFICE_CODE, ClaimsGatewayStub.MULTIPLE_CLAIMS_UFN)
            .getClaims();

    assertEquals(2, claims.size());
    assertEquals(ClaimsGatewayStub.RECENT_CLAIM_ID, claims.get(0).getClaimId());
    assertEquals(ClaimsGatewayStub.EARLIER_CLAIM_ID, claims.get(1).getClaimId());
  }

  @Test
  void unknownApplication_returnsOneClaimUpdatedNow() {
    final var first = stub.getClaims("OTHER", "999999/001").getClaims();
    final var second = stub.getClaims("OTHER", "999999/001").getClaims();

    assertEquals(1, first.size());
    assertNotEquals(first.get(0).getClaimId(), second.get(0).getClaimId());
    assertTrue(first.get(0).getUpdatedOn().isAfter(ClaimsGatewayStub.RECENT_CLAIM_UPDATED_ON));
  }
}
