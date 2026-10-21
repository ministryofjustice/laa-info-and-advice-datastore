package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;
import uk.gov.justice.laa.ia.datastore.config.ClaimsApiProperties;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsApiGateway;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.service.RetentionDateUpdateService;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/**
 * Runs the retention date resolver against the real claims API UAT environment, using office codes
 * and UFNs from the UAT claims data. Only runs locally when CLAIMS_API_KEY is set and you are on
 * the VPN; it is skipped otherwise. CLAIMS_API_URL can override the default UAT URL. The claims
 * team may reset the UAT data, in which case the expected dates below need updating.
 */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@EnabledIfEnvironmentVariable(named = "CLAIMS_API_KEY", matches = ".+")
class DataRetentionDateResolverTaskUatIntegrationTest extends BaseIntegrationTest {
  private static final String DEFAULT_URL =
      "https://main-laa-data-claims-api-uat.cloud-platform.service.justice.gov.uk";
  private static final int DATA_RETENTION_YEARS_OFFSET = 3;

  private DataRetentionDateResolverTask sut;

  @BeforeEach
  void setUpResolver() {
    final var url = System.getenv().getOrDefault("CLAIMS_API_URL", DEFAULT_URL);
    final var gateway =
        new ClaimsApiGateway(
            RestClient.builder(),
            new ClaimsApiProperties(
                ClaimsApiProperties.LIVE_MODE,
                url.isBlank() ? DEFAULT_URL : url,
                System.getenv("CLAIMS_API_KEY"),
                20,
                Duration.ofSeconds(5),
                Duration.ofSeconds(30)));
    this.sut =
        new DataRetentionDateResolverTask(
            gateway,
            DATA_RETENTION_YEARS_OFFSET,
            applicationRepository,
            new RetentionDateUpdateService(
                applicationRepository,
                new SystemDrivenEventService(eventRepository, objectMapper, new AuditLogger())));
  }

  private ApplicationEntity saveCompletedApplication(String officeCode, String ufn) {
    return applicationRepository.saveAndFlush(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(null)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode)));
  }

  private Instant retentionDateOf(ApplicationEntity application) {
    return applicationRepository.findById(application.getId()).orElseThrow().getDataRetentionDate();
  }

  @Test
  void validClaim_setsRetentionFromClaimUpdatedOn() {
    final var application = saveCompletedApplication("0A746L", "010119/237");

    sut.run();

    assertEquals(Instant.parse("2029-01-15T11:53:11.227957Z"), retentionDateOf(application));
  }

  @Test
  void validClaimAlongsideNewerInvalidClaim_setsRetentionFromValidClaim() {
    final var application = saveCompletedApplication("0P322F", "010119/004");

    sut.run();

    assertEquals(Instant.parse("2029-01-26T14:35:00.713191Z"), retentionDateOf(application));
  }

  @Test
  void noClaims_leavesRetentionUnset() {
    final var application = saveCompletedApplication("0A746L", "999999/999");

    sut.run();

    assertNull(retentionDateOf(application));
  }

  @Test
  void voidClaim_leavesRetentionUnsetBecauseApiOmitsVoidClaims() {
    final var voidOnly = saveCompletedApplication("0P322F", "030120/033");
    final var invalidThenVoid = saveCompletedApplication("0P322F", "010119/001");

    sut.run();

    assertNull(retentionDateOf(voidOnly));
    assertNull(retentionDateOf(invalidThenVoid));
  }
}
