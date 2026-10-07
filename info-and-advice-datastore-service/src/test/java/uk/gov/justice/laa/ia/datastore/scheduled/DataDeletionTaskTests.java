package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;
import uk.gov.justice.laa.ia.datastore.models.ClaimsModel;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;
import uk.gov.justice.laa.ia.datastore.service.RetentionDateUpdateService;

/** Unit tests for the {@link DataDeletionTask}. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@ExtendWith(MockitoExtension.class)
class DataDeletionTaskTests {

  private static final int DATA_RETENTION_YEARS_OFFSET = 3;
  private static final String OFFICE_CODE = "test-office-code";

  @Mock private ApplicationRepository applicationRepository;
  @Mock private ClaimsGateway claimsGateway;
  @Mock private RetentionDateUpdateService retentionDateUpdateService;
  @Mock private ApplicationDataDeletionService applicationDataDeletionService;

  private DataDeletionTask sut;

  @BeforeEach
  void setUp() {
    this.sut =
        new DataDeletionTask(
            applicationRepository,
            claimsGateway,
            DATA_RETENTION_YEARS_OFFSET,
            retentionDateUpdateService,
            applicationDataDeletionService);
  }

  @SuppressWarnings("unchecked")
  private static Specification<ApplicationEntity> anySpecification() {
    return any(Specification.class);
  }

  @Test
  void givenNoApplicationsWithExpiredRetentionDate_thenDoNothing() {
    // Arrange
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of());

    // Act
    sut.run();

    // Assert
    verify(retentionDateUpdateService, never())
        .saveAndRecord(any(), anyLong(), any(), any(), any());
    verify(applicationDataDeletionService, never()).deleteAndRecord(any(), anyLong());
  }

  @Test
  void givenClaimsGatewayThrowsException_thenSkipApplicationEntirely() {
    // Arrange
    final String ufn = "111111/1";
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().ufn(ufn).providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));
    when(claimsGateway.getClaims(OFFICE_CODE, ufn))
        .thenThrow(new RuntimeException("Claims API unavailable"));

    // Act
    sut.run();

    // Assert
    verify(retentionDateUpdateService, never())
        .saveAndRecord(any(), anyLong(), any(), any(), any());
    verify(applicationDataDeletionService, never()).deleteAndRecord(any(), anyLong());
  }

  @Test
  void givenLatestClaimApprovedAndStillWithinRetention_thenExtendRetentionDate() {
    // Arrange
    final String ufn = "222222/1";
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().ufn(ufn).providerOfficeCode(OFFICE_CODE));
    final OffsetDateTime claimCreatedOn = OffsetDateTime.now().minusYears(1);
    final UUID claimId = UUID.randomUUID();
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));
    when(claimsGateway.getClaims(OFFICE_CODE, ufn))
        .thenReturn(
            ApplicationClaimResponse.builder()
                .claims(
                    List.of(
                        ClaimsModel.builder()
                            .claimId(claimId)
                            .status("APPROVED")
                            .createdOn(claimCreatedOn)
                            .build()))
                .build());

    // Act
    sut.run();

    // Assert
    verify(retentionDateUpdateService, times(1))
        .saveAndRecord(
            eq(application.getId()), eq(application.getEtag()), any(), any(), eq(claimId));
    verify(applicationDataDeletionService, never()).deleteAndRecord(any(), anyLong());
  }

  @Test
  void givenLatestClaimApprovedButStillExpiredAfterRecompute_thenDeleteApplication() {
    // Arrange
    final String ufn = "333333/1";
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().ufn(ufn).providerOfficeCode(OFFICE_CODE));
    final OffsetDateTime claimCreatedOn = OffsetDateTime.now().minusYears(5);
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));
    when(claimsGateway.getClaims(OFFICE_CODE, ufn))
        .thenReturn(
            ApplicationClaimResponse.builder()
                .claims(
                    List.of(
                        ClaimsModel.builder()
                            .claimId(UUID.randomUUID())
                            .status("APPROVED")
                            .createdOn(claimCreatedOn)
                            .build()))
                .build());

    // Act
    sut.run();

    // Assert
    verify(applicationDataDeletionService, times(1))
        .deleteAndRecord(application.getId(), application.getEtag());
    verify(retentionDateUpdateService, never())
        .saveAndRecord(any(), anyLong(), any(), any(), any());
  }

  @Test
  void givenLatestClaimNotApproved_thenDeleteApplication() {
    // Arrange
    final String ufn = "444444/1";
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().ufn(ufn).providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));
    when(claimsGateway.getClaims(OFFICE_CODE, ufn))
        .thenReturn(
            ApplicationClaimResponse.builder()
                .claims(
                    List.of(
                        ClaimsModel.builder()
                            .status("REJECTED")
                            .createdOn(OffsetDateTime.now())
                            .build()))
                .build());

    // Act
    sut.run();

    // Assert
    verify(applicationDataDeletionService, times(1))
        .deleteAndRecord(application.getId(), application.getEtag());
  }

  @Test
  void givenNoClaims_thenSkipApplicationWithoutUpdateOrDelete() {
    // Arrange
    final String ufn = "555555/1";
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().ufn(ufn).providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));
    when(claimsGateway.getClaims(OFFICE_CODE, ufn))
        .thenReturn(ApplicationClaimResponse.builder().claims(List.of()).build());

    // Act
    sut.run();

    // Assert
    verify(retentionDateUpdateService, never())
        .saveAndRecord(any(), anyLong(), any(), any(), any());
    verify(applicationDataDeletionService, never()).deleteAndRecord(any(), anyLong());
  }

  @Test
  void givenLatestClaimIsNull_thenSkipApplicationWithoutUpdateOrDelete() {
    // Arrange
    final String ufn = "999999/1";
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().ufn(ufn).providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));
    when(claimsGateway.getClaims(OFFICE_CODE, ufn))
        .thenReturn(ApplicationClaimResponse.builder().claims(null).build());

    // Act
    sut.run();

    // Assert
    verify(retentionDateUpdateService, never())
        .saveAndRecord(any(), anyLong(), any(), any(), any());
    verify(applicationDataDeletionService, never()).deleteAndRecord(any(), anyLong());
  }

  @Test
  void givenOneApplicationThrows_thenContinueProcessingOthers() {
    // Arrange
    final String failingUfn = "666666/1";
    final String succeedingUfn = "777777/1";
    final ApplicationEntity failingApplication =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder.withDefaultClientDetails().ufn(failingUfn).providerOfficeCode(OFFICE_CODE));
    final ApplicationEntity succeedingApplication =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(succeedingUfn)
                    .providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification()))
        .thenReturn(List.of(failingApplication, succeedingApplication));
    when(claimsGateway.getClaims(OFFICE_CODE, failingUfn))
        .thenReturn(
            ApplicationClaimResponse.builder()
                .claims(
                    List.of(
                        ClaimsModel.builder()
                            .status("REJECTED")
                            .createdOn(OffsetDateTime.now())
                            .build()))
                .build());
    when(claimsGateway.getClaims(OFFICE_CODE, succeedingUfn))
        .thenReturn(
            ApplicationClaimResponse.builder()
                .claims(
                    List.of(
                        ClaimsModel.builder()
                            .status("REJECTED")
                            .createdOn(OffsetDateTime.now())
                            .build()))
                .build());
    doThrow(new RuntimeException("DB constraint violation"))
        .when(applicationDataDeletionService)
        .deleteAndRecord(eq(failingApplication.getId()), anyLong());

    // Act
    sut.run();

    // Assert
    verify(applicationDataDeletionService, times(1))
        .deleteAndRecord(failingApplication.getId(), failingApplication.getEtag());
    verify(applicationDataDeletionService, times(1))
        .deleteAndRecord(succeedingApplication.getId(), succeedingApplication.getEtag());
  }
}
