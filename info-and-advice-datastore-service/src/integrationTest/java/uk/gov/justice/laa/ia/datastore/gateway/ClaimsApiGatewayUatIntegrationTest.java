package uk.gov.justice.laa.ia.datastore.gateway;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.ia.datastore.config.ClaimsApiProperties;

/**
 * Calls the real claims API UAT environment. Only runs when CLAIMS_API_KEY is set, so it is skipped
 * in the normal Gradle test runs. The UAT deploy workflow runs it inside the cluster (see
 * deploy/scripts/run-claims-api-test-in-cluster.sh), because the public UAT ingress is only
 * reachable from the VPN. CLAIMS_API_URL can override the default UAT URL.
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
  void unknownOfficeAndUfnReturnsSuccessfulEmptyResponse() {
    // A 4xx/5xx would throw, so reaching the assertion proves a 200.
    final var response = gateway().getClaims("000000", "010120/001");

    assertNotNull(response.getClaims());
  }

  @Test
  void knownOfficeAndUfnReturnsValidClaims() {
    final var response = gateway().getClaims("0A746L", "010119/237");

    assertFalse(response.getClaims().isEmpty());
    response.getClaims().forEach(claim -> assertNotNull(claim.getUpdatedOn()));
  }
}
