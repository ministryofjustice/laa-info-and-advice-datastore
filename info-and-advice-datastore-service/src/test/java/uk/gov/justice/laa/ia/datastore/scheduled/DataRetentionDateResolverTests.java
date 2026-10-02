package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

/** Unit tests for the {@link DataRetentionDateResolver}. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@ExtendWith(MockitoExtension.class)
class DataRetentionDateResolverTests {

  private static final int DATA_RETENTION_YEARS_OFFSET = 3;
  private static final String OFFICE_CODE = "test-office-code";

  @Mock private ClaimsGateway claimsGateway;
  @Mock private ApplicationRepository applicationRepository;
  @Mock private RetentionDateUpdateService retentionDateUpdateService;

  private DataRetentionDateResolver sut;

  @BeforeEach
  void setUp() {
    this.sut =
        new DataRetentionDateResolver(
            claimsGateway,
            DATA_RETENTION_YEARS_OFFSET,
            applicationRepository,
            retentionDateUpdateService);
  }

  @SuppressWarnings("unchecked")
  private static Specification<ApplicationEntity> anySpecification() {
    return any(Specification.class);
  }

  @Test
  void givenNoApplicationsMissingRetentionDate_thenDoNothing() {
    // Arrange
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of());

    // Act
    sut.run();

    // Assert
    verify(applicationRepository, never()).save(any());
  }

  @Test
  void givenClaimsResponseIsNull_thenDoNotSetDataRetentionDate() {
    // Arrange
    final String ufn = "111111/1";
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(null)
                    .providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));
    when(claimsGateway.getClaims(OFFICE_CODE, ufn)).thenReturn(null);

    // Act
    sut.run();

    // Assert
    verify(applicationRepository, never()).save(any());
  }

  @Test
  void givenClaimsListIsNull_thenDoNotSetDataRetentionDate() {
    // Arrange
    final String ufn = "222222/1";
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(null)
                    .providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification())).thenReturn(List.of(application));
    when(claimsGateway.getClaims(OFFICE_CODE, ufn))
        .thenReturn(ApplicationClaimResponse.builder().claims(null).build());

    // Act
    sut.run();

    // Assert
    verify(applicationRepository, never()).save(any());
  }

  @Test
  void givenClaimsGatewayThrowsException_thenSkipApplicationAndContinue() {
    // Arrange
    final String failingUfn = "333333/1";
    final String succeedingUfn = "444444/1";
    final ApplicationEntity failingApplication =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(failingUfn)
                    .dataRetentionDate(null)
                    .providerOfficeCode(OFFICE_CODE));
    final ApplicationEntity succeedingApplication =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(succeedingUfn)
                    .dataRetentionDate(null)
                    .providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification()))
        .thenReturn(List.of(failingApplication, succeedingApplication));
    when(claimsGateway.getClaims(OFFICE_CODE, failingUfn))
        .thenThrow(new RuntimeException("Claims API unavailable"));
    final OffsetDateTime claimApprovedDate =
        OffsetDateTime.of(2026, 10, 1, 8, 30, 0, 0, ZoneOffset.UTC);
    when(claimsGateway.getClaims(OFFICE_CODE, succeedingUfn))
        .thenReturn(
            ApplicationClaimResponse.builder()
                .claims(
                    List.of(
                        ClaimsModel.builder()
                            .status("APPROVED")
                            .createdOn(claimApprovedDate)
                            .updatedOn(claimApprovedDate)
                            .build()))
                .build());

    // Act
    sut.run();

    // Assert
    final ArgumentCaptor<UUID> savedIdCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(retentionDateUpdateService, times(1))
        .saveAndRecord(savedIdCaptor.capture(), anyLong(), any(), any(), any());
    assertThat(savedIdCaptor.getValue()).isEqualTo(succeedingApplication.getId());
  }

  @Test
  void givenSaveAndRecordThrowsExceptionForOneApplication_thenContinueProcessingOthers() {
    // Arrange
    final String failingUfn = "555555/1";
    final String succeedingUfn = "666666/1";
    final ApplicationEntity failingApplication =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(failingUfn)
                    .dataRetentionDate(null)
                    .providerOfficeCode(OFFICE_CODE));
    final ApplicationEntity succeedingApplication =
        ApplicationEntityGenerator.createWithId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(succeedingUfn)
                    .dataRetentionDate(null)
                    .providerOfficeCode(OFFICE_CODE));
    when(applicationRepository.findAll(anySpecification()))
        .thenReturn(List.of(failingApplication, succeedingApplication));
    final OffsetDateTime claimApprovedDate =
        OffsetDateTime.of(2026, 10, 1, 8, 30, 0, 0, ZoneOffset.UTC);
    final ClaimsModel approvedClaim =
        ClaimsModel.builder()
            .status("APPROVED")
            .createdOn(claimApprovedDate)
            .updatedOn(claimApprovedDate)
            .build();
    when(claimsGateway.getClaims(OFFICE_CODE, failingUfn))
        .thenReturn(ApplicationClaimResponse.builder().claims(List.of(approvedClaim)).build());
    when(claimsGateway.getClaims(OFFICE_CODE, succeedingUfn))
        .thenReturn(ApplicationClaimResponse.builder().claims(List.of(approvedClaim)).build());
    doThrow(new RuntimeException("DB constraint violation"))
        .when(retentionDateUpdateService)
        .saveAndRecord(eq(failingApplication.getId()), anyLong(), any(), any(), any());

    // Act
    sut.run();

    // Assert
    verify(retentionDateUpdateService, times(1))
        .saveAndRecord(eq(failingApplication.getId()), anyLong(), any(), any(), any());
    verify(retentionDateUpdateService, times(1))
        .saveAndRecord(eq(succeedingApplication.getId()), anyLong(), any(), any(), any());
  }
}
