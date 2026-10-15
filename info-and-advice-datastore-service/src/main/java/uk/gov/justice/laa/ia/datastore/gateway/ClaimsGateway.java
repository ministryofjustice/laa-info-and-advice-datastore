package uk.gov.justice.laa.ia.datastore.gateway;

import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;

/** Gateway interface for interacting with the Claim API. */
public interface ClaimsGateway {

  ApplicationClaimResponse getClaims(String officeCode, String ufn);
}
