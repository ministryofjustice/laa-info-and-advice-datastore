package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.gateway.stub.ClaimsGatewayStub;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.service.RetentionDateUpdateService;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the DataRetentionDateResolverTask scheduled task. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@ExtendWith(MockitoExtension.class)
public class DataRetentionDateResolverTaskIntegrationTest extends BaseIntegrationTest {

  private DataRetentionDateResolverTask sut;
  @Spy private ClaimsGatewayStub claimsGateway = new ClaimsGatewayStub();
  static final String officeCode = ClaimsGatewayStub.FIXTURE_OFFICE_CODE;
  static final int DATA_RETENTION_YEARS_OFFSET = 3;

  @BeforeEach
  void setUpResolver() {
    this.sut =
        new DataRetentionDateResolverTask(
            claimsGateway,
            DATA_RETENTION_YEARS_OFFSET,
            applicationRepository,
            new RetentionDateUpdateService(
                applicationRepository,
                new SystemDrivenEventService(eventRepository, objectMapper)));
  }

  @Test
  void givenApplicationWithNoClaims_thenDoNotSetDataRetentionDate() {
    // Arrange

    final String ufn = ClaimsGatewayStub.NO_CLAIMS_UFN;

    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(null)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(application);

    // Act
    sut.run();

    // Assert
    var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertNull(savedApplication.getDataRetentionDate());
    verify(claimsGateway, times(1)).getClaims(officeCode, ufn);
  }

  @Test
  void givenApplicationWithoutUfn_thenSkipApplicationWithoutCallingClaimsApi() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(null)
                    .dataRetentionDate(null)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(application);

    // Act
    sut.run();

    // Assert
    var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertNull(savedApplication.getDataRetentionDate());
    verify(claimsGateway, never()).getClaims(any(), any());
  }

  @Test
  void givenApplicationWithClaim_thenSetDataRetentionDateToConfiguredOffset() {
    // Arrange
    final Instant expectedDataRetentionDate =
        ClaimsGatewayStub.RECENT_CLAIM_UPDATED_ON
            .plusYears(DATA_RETENTION_YEARS_OFFSET)
            .toInstant();
    final String ufn = ClaimsGatewayStub.RECENT_CLAIM_UFN;

    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(null)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(application);

    // Act
    sut.run();

    // Assert
    var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertNotNull(savedApplication.getDataRetentionDate());
    assertEquals(expectedDataRetentionDate, savedApplication.getDataRetentionDate());
    verify(claimsGateway, times(1)).getClaims(officeCode, ufn);

    var events = eventRepository.findAll();
    assertEquals(1, events.size());
    var event = events.get(0);
    assertEquals(officeCode, event.getProviderOfficeCode());
    assertEquals("SYSTEM", event.getChangedBy());
    assertEquals(DataRetentionDateResolverTask.class.getSimpleName(), event.getUrlPath());
    assertEquals(
        expectedDataRetentionDate.toString(), event.getPayload().get("dataRetentionDate").asText());
  }

  @Test
  void givenApplicationWithMultipleClaims_thenSetDataRetentionDateFromLatestClaim() {
    // Arrange
    final Instant expectedDataRetentionDate =
        ClaimsGatewayStub.RECENT_CLAIM_UPDATED_ON
            .plusYears(DATA_RETENTION_YEARS_OFFSET)
            .toInstant();
    final String ufn = ClaimsGatewayStub.MULTIPLE_CLAIMS_UFN;

    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(null)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(application);

    // Act
    sut.run();

    // Assert
    var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertNotNull(savedApplication.getDataRetentionDate());
    assertEquals(expectedDataRetentionDate, savedApplication.getDataRetentionDate());
    verify(claimsGateway, times(1)).getClaims(officeCode, ufn);
  }

  @Test
  void givenApplicationAlreadyHasRententionDate_thenDoNotTryToGetClaims() {
    // Arrange
    final Instant existingDataRetentionDate = Instant.now();
    final String ufn = ClaimsGatewayStub.RECENT_CLAIM_UFN;
    final String ufnWithRetentionDate = ClaimsGatewayStub.NO_CLAIMS_UFN;
    final ApplicationEntity applicationWithRetentionDate =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufnWithRetentionDate)
                    .dataRetentionDate(existingDataRetentionDate)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    final ApplicationEntity applicationWithoutRetentionDate =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(null)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(applicationWithRetentionDate);
    applicationRepository.saveAndFlush(applicationWithoutRetentionDate);

    // Act
    sut.run();

    // Assert
    verify(claimsGateway, times(0)).getClaims(officeCode, ufnWithRetentionDate);
    verify(claimsGateway, times(1)).getClaims(officeCode, ufn);
  }

  @Test
  void givenApplicationIsNotCompleted_thenDoNotResolveDataRetentionDate() {
    // Arrange
    final String ufn = ClaimsGatewayStub.RECENT_CLAIM_UFN;
    final ApplicationEntity draftApplication =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(null)
                    .applicationState(ApplicationState.DRAFT)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(draftApplication);

    // Act
    sut.run();

    // Assert
    var savedApplication = applicationRepository.findById(draftApplication.getId()).orElseThrow();
    assertNull(savedApplication.getDataRetentionDate());
    verify(claimsGateway, times(0)).getClaims(officeCode, ufn);
  }
}
