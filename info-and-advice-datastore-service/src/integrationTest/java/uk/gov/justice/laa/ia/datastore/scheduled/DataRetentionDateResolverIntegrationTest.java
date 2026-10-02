package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;
import uk.gov.justice.laa.ia.datastore.models.ClaimsModel;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the DataRetentionDateResolver scheduled task. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@ExtendWith(MockitoExtension.class)
public class DataRetentionDateResolverIntegrationTest extends BaseIntegrationTest {

  private DataRetentionDateResolver sut;
  @Mock private ClaimsGateway claimsGateway;
  static final String officeCode = "test-office-code-for-data-retention";
  static final int DATA_RETENTION_YEARS_OFFSET = 3;

  @BeforeEach
  void setUpResolver() {
    this.sut =
        new DataRetentionDateResolver(
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

    final String ufn = "111111/1";
    when(claimsGateway.getClaims(officeCode, ufn))
        .thenReturn(ApplicationClaimResponse.builder().claims(List.of()).build());

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
  void givenApplicationWithClaimButNotApproved_thenDoNotSetDataRetentionDate() {
    // Arrange
    final ClaimsModel claim = ClaimsModel.builder().status("NOT_APPROVED").build();
    final String ufn = "222222/1";
    when(claimsGateway.getClaims(officeCode, ufn))
        .thenReturn(ApplicationClaimResponse.builder().claims(List.of(claim)).build());

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
  void givenApplicationWithClaimThatIsApproved_thenSetDataRetentionDateToConfiguredOffset() {
    // Arrange
    final OffsetDateTime claimApprovedDate =
        OffsetDateTime.of(2026, 10, 1, 8, 30, 0, 0, ZoneOffset.UTC);
    final Instant expectedDataRetentionDate =
        OffsetDateTime.of(2029, 10, 1, 8, 30, 0, 0, ZoneOffset.UTC).toInstant();
    final ClaimsModel claim =
        ClaimsModel.builder()
            .status("APPROVED")
            .createdOn(claimApprovedDate)
            .updatedOn(claimApprovedDate)
            .build();
    final String ufn = "333333/1";
    when(claimsGateway.getClaims(officeCode, ufn))
        .thenReturn(ApplicationClaimResponse.builder().claims(List.of(claim)).build());

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
    assertEquals(DataRetentionDateResolver.class.getSimpleName(), event.getUrlPath());
    assertEquals(
        expectedDataRetentionDate.toString(), event.getPayload().get("dataRetentionDate").asText());
  }

  @Test
  void givenApplicationWithMultipleClaims_whenLatestClaimIsApproved_thenSetDataRetentionDate() {
    // Arrange
    final OffsetDateTime claimApprovedDate =
        OffsetDateTime.of(2026, 10, 1, 8, 30, 0, 0, ZoneOffset.UTC);
    final OffsetDateTime earlierClaimDate =
        OffsetDateTime.of(2026, 9, 1, 8, 30, 0, 0, ZoneOffset.UTC);
    final Instant expectedDataRetentionDate =
        OffsetDateTime.of(2029, 10, 1, 8, 30, 0, 0, ZoneOffset.UTC).toInstant();
    final ClaimsModel claim =
        ClaimsModel.builder()
            .status("APPROVED")
            .createdOn(claimApprovedDate)
            .updatedOn(claimApprovedDate)
            .build();
    final ClaimsModel earlierClaim =
        ClaimsModel.builder()
            .status("NOT APPROVED")
            .createdOn(earlierClaimDate)
            .updatedOn(earlierClaimDate)
            .build();
    final String ufn = "444444/1";
    when(claimsGateway.getClaims(officeCode, ufn))
        .thenReturn(
            ApplicationClaimResponse.builder().claims(List.of(claim, earlierClaim)).build());

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
    final String ufn = "555555/1";
    final String ufnWithRetentionDate = "666666/1";
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
    final String ufn = "777777/1";
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
