package uk.gov.justice.laa.ia.datastore.gateway.stub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;
import uk.gov.justice.laa.ia.datastore.models.ClaimsModel;

/**
 * Stub implementation of the ClaimsGateway interface. Office code and UFN pairs in the fixtures
 * return fixed claims so tests can rely on them; every other pair returns a single VALID claim
 * updated now.
 */
@ExcludeFromCodeCoverage(reason = "Stub implementation for testing purposes")
@Component
@ConditionalOnProperty(
    name = "laa.datastore.claims-api.mode",
    havingValue = "stub",
    matchIfMissing = true)
public class ClaimsGatewayStub implements ClaimsGateway {

  /** Office code shared by all fixtures. */
  public static final String FIXTURE_OFFICE_CODE = "STUB01";

  /** Fixture with no claims. */
  public static final String NO_CLAIMS_UFN = "100000/001";

  /** Fixture with one claim last updated long enough ago for its retention period to have ended. */
  public static final String OLD_CLAIM_UFN = "200000/001";

  /** Fixture with one claim updated recently. */
  public static final String RECENT_CLAIM_UFN = "300000/001";

  /** Fixture with two claims, the recent one listed first. */
  public static final String MULTIPLE_CLAIMS_UFN = "400000/001";

  public static final UUID OLD_CLAIM_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  public static final UUID RECENT_CLAIM_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000003");
  public static final UUID EARLIER_CLAIM_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000004");

  public static final OffsetDateTime OLD_CLAIM_UPDATED_ON =
      OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
  public static final OffsetDateTime RECENT_CLAIM_UPDATED_ON =
      OffsetDateTime.of(2026, 10, 1, 8, 30, 0, 0, ZoneOffset.UTC);
  public static final OffsetDateTime EARLIER_CLAIM_UPDATED_ON =
      OffsetDateTime.of(2026, 9, 1, 8, 30, 0, 0, ZoneOffset.UTC);

  private record Key(String officeCode, String ufn) {}

  private static final Map<Key, List<ClaimsModel>> FIXTURES =
      Map.of(
          new Key(FIXTURE_OFFICE_CODE, NO_CLAIMS_UFN), List.of(),
          new Key(FIXTURE_OFFICE_CODE, OLD_CLAIM_UFN),
              List.of(claim(OLD_CLAIM_ID, OLD_CLAIM_UPDATED_ON)),
          new Key(FIXTURE_OFFICE_CODE, RECENT_CLAIM_UFN),
              List.of(claim(RECENT_CLAIM_ID, RECENT_CLAIM_UPDATED_ON)),
          new Key(FIXTURE_OFFICE_CODE, MULTIPLE_CLAIMS_UFN),
              List.of(
                  claim(RECENT_CLAIM_ID, RECENT_CLAIM_UPDATED_ON),
                  claim(EARLIER_CLAIM_ID, EARLIER_CLAIM_UPDATED_ON)));

  @Override
  public ApplicationClaimResponse getClaims(String officeCode, String ufn) {
    final List<ClaimsModel> fixture = FIXTURES.get(new Key(officeCode, ufn));
    if (fixture != null) {
      return ApplicationClaimResponse.builder().claims(fixture).build();
    }
    return ApplicationClaimResponse.builder()
        .claims(List.of(claim(UUID.randomUUID(), OffsetDateTime.now())))
        .build();
  }

  private static ClaimsModel claim(UUID claimId, OffsetDateTime updatedOn) {
    return ClaimsModel.builder()
        .claimId(claimId)
        .status("VALID")
        .createdOn(updatedOn)
        .updatedOn(updatedOn)
        .build();
  }
}
