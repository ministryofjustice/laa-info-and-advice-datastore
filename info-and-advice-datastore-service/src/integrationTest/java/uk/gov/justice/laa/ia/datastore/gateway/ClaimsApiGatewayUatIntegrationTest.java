package uk.gov.justice.laa.ia.datastore.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.ia.datastore.config.ClaimsApiProperties;

/**
 * Calls the real claims API UAT environment, so it only runs locally when CLAIMS_API_KEY is set and
 * you are on the VPN (the public UAT ingress is not reachable from CI). It is skipped otherwise.
 * CLAIMS_API_URL can override the default UAT URL. The office codes and UFNs below come from the
 * UAT claims data, which the claims team may reset.
 */
@EnabledIfEnvironmentVariable(named = "CLAIMS_API_KEY", matches = ".+")
class ClaimsApiGatewayUatIntegrationTest {
  private static final String DEFAULT_URL =
      "https://main-laa-data-claims-api-uat.cloud-platform.service.justice.gov.uk";

  private ClaimsApiGateway gateway() {
    final var url = System.getenv().getOrDefault("CLAIMS_API_URL", DEFAULT_URL);
    return new ClaimsApiGateway(
        RestClient.builder(),
        new ClaimsApiProperties(
            ClaimsApiProperties.LIVE_MODE,
            url.isBlank() ? DEFAULT_URL : url,
            System.getenv("CLAIMS_API_KEY"),
            20,
            Duration.ofSeconds(5),
            Duration.ofSeconds(30)));
  }

  @Test
  void validClaim_isReturnedWithRetentionFields() {
    final var claims = gateway().getClaims("0A746L", "010119/237").getClaims();

    assertEquals(1, claims.size());
    assertNotNull(claims.get(0).getClaimId());
    assertEquals("VALID", claims.get(0).getStatus());
    assertNotNull(claims.get(0).getUpdatedOn());
  }

  @Test
  void unknownOfficeAndUfn_returnsSuccessfulEmptyResponse() {
    // A 4xx/5xx would throw, so an empty list proves a 200.
    assertTrue(gateway().getClaims("0A746L", "999999/999").getClaims().isEmpty());
  }

  @Test
  void voidClaim_isNotReturned() {
    assertTrue(gateway().getClaims("0P322F", "030120/033").getClaims().isEmpty());
    assertTrue(gateway().getClaims("1T102C", "010115/495").getClaims().isEmpty());
  }
}
