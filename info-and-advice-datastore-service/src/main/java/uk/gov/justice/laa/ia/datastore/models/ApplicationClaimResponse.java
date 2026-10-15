package uk.gov.justice.laa.ia.datastore.models;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/** API response from Claim API. * */
@Builder
@Getter
@Setter
public class ApplicationClaimResponse {
  private List<ClaimsModel> claims;
}
