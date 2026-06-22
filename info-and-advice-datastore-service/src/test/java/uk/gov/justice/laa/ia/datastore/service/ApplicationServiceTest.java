package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.UpdateEvidenceGenerator;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.mapper.DeclarationMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;

/** Unit tests for the {@link ApplicationService}. */
@ExtendWith(MockitoExtension.class)
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ApplicationServiceTest {
  @Mock private ApplicationRepository repo;
  @Mock private EligibilityResultRepository eligibilityResultRepository;
  @Mock private ApplicationMapper mapper;
  @Mock private DeclarationMapper declarationMapper;
  @Mock private UserContext userContext;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private ApplicationService sut;

  @Test
  void shouldCreateApplication() {
    // Arrange
    final StartCaseCommand cmd = new StartCaseCommand();
    final ApplicationEntity entity = new ApplicationEntity();
    final UUID generatedId = UUID.randomUUID();
    final UUID firmId = UUID.randomUUID();
    final UUID officeId = UUID.randomUUID();
    final String user = "TEST_USER";

    when(mapper.toApplicationEntity(cmd)).thenReturn(entity);
    when(userContext.getProviderFirmId()).thenReturn(firmId);
    when(userContext.getProviderOfficeId()).thenReturn(officeId);
    when(userContext.getCurrentUser()).thenReturn(user);
    when(repo.save(any(ApplicationEntity.class)))
        .thenAnswer(
            invocation -> {
              ApplicationEntity saved = invocation.getArgument(0);
              saved.setId(generatedId);
              return saved;
            });

    // Act
    final UUID result = sut.createApplication(cmd);

    // Assert
    assertThat(result).isEqualTo(generatedId);
    assertThat(entity.getProviderFirmId()).isEqualTo(firmId);
    assertThat(entity.getProviderOfficeId()).isEqualTo(officeId);
    assertThat(entity.getApplicationState()).isEqualTo(ApplicationState.DRAFT);
    assertThat(entity.getCreatedBy()).isEqualTo(user);
    assertThat(entity.getModifiedBy()).isEqualTo(user);

    verify(repo, times(1)).save(entity);
  }

  @Test
  void shouldGetAllApplications() {
    // Arrange
    final ApplicationEntity entity1 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationEntity entity2 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationResponse application1 =
        ApplicationResponse.builder().id(entity1.getId()).build();
    final ApplicationResponse application2 =
        ApplicationResponse.builder().id(entity2.getId()).build();
    when(repo.findAll(any(org.springframework.data.domain.Pageable.class)))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entity1, entity2)));
    when(mapper.toApplication(entity1)).thenReturn(application1);
    when(mapper.toApplication(entity2)).thenReturn(application2);

    // Act
    final List<ApplicationResponse> result = sut.getAllApplications(null, null);

    // Assert
    assertThat(result).hasSize(2).contains(application1, application2);
    verify(repo, times(1)).findAll(any(org.springframework.data.domain.Pageable.class));
    verify(mapper, times(2)).toApplication(any());
  }

  @Test
  void shouldGetApplication() {
    // Arrange
    final ApplicationEntity entity = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationResponse application =
        ApplicationResponse.builder().id(entity.getId()).build();
    when(repo.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(userContext.canAccessApplication(entity)).thenReturn(true);
    when(mapper.toApplication(entity)).thenReturn(application);

    // Act
    final ApplicationResponse result = sut.getApplication(entity.getId()).orElseThrow();

    // Assert
    assertThat(result).isEqualTo(application);
    verify(repo, times(1)).findById(entity.getId());
    verify(mapper, times(1)).toApplication(entity);
  }

  @Test
  void shouldReturnEmptyOptional_whenAccessDenied() {
    // Arrange
    final ApplicationEntity entity = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    when(repo.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(userContext.canAccessApplication(entity)).thenReturn(false);

    // Act
    Optional<ApplicationResponse> result = sut.getApplication(entity.getId());

    // Assert
    assertThat(result).isEmpty();
    verify(repo, times(1)).findById(entity.getId());
    verify(mapper, times(0)).toApplication(any());
  }

  @Test
  void shouldReturnEmptyOptional_whenApplicationDoesNotExist() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    Optional<ApplicationResponse> result = sut.getApplication(UUID.randomUUID());

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldUpdateMeansData() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final Instant originalModifiedAt = Instant.now().minusSeconds(60);
    final UUID originalMeansAssessmentId = UUID.randomUUID();
    final ApplicationEntity application =
        ApplicationEntity.builder()
            .id(applicationId)
            .meansAssessmentId(originalMeansAssessmentId)
            .modifiedAt(originalModifiedAt)
            .build();
    final Object body = new Object();
    final JsonNode jsonNode = new ObjectMapper().createObjectNode();
    final String user = "TEST_USER";

    when(repo.findById(applicationId)).thenReturn(Optional.of(application));
    when(userContext.canAccessApplication(application)).thenReturn(true);
    when(userContext.getCurrentUser()).thenReturn(user);
    when(objectMapper.valueToTree(body)).thenReturn(jsonNode);

    // Act
    boolean result = sut.updateMeansData(applicationId, body);

    // Assert
    assertTrue(result);
    verify(eligibilityResultRepository, times(1)).save(any(EligibilityResultEntity.class));
    verify(repo, times(1)).save(application);
    assertThat(application.getModifiedBy()).isEqualTo(user);
    assertThat(application.getModifiedAt()).isAfter(originalModifiedAt);

    ArgumentCaptor<EligibilityResultEntity> resultCaptor =
        ArgumentCaptor.forClass(EligibilityResultEntity.class);
    verify(eligibilityResultRepository).save(resultCaptor.capture());
    assertThat(resultCaptor.getValue().getApplicationId()).isEqualTo(applicationId);
    assertThat(resultCaptor.getValue().getResultJson()).isEqualTo(jsonNode);
    assertThat(application.getMeansAssessmentId()).isEqualTo(originalMeansAssessmentId);
  }

  @Test
  void shouldReturnFalse_whenUpdatingMeansDataForUnknownApplication() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final Object body = new Object();
    when(repo.findById(applicationId)).thenReturn(Optional.empty());

    // Act
    boolean result = sut.updateMeansData(applicationId, body);

    // Assert
    assertFalse(result);
    verify(eligibilityResultRepository, never()).save(any(EligibilityResultEntity.class));
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void updateDeclaration_shouldCreateDeclaration_whenApplicationHasNoDeclaration() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final DeclarationCommand declarationCommand =
        DeclarationCommand.builder().declarationConfirmation(true).build();
    final DeclarationEntity declarationEntity =
        DeclarationEntityGenerator.createWithId(builder -> builder.declarationConfirmation(true));
    final ApplicationEntity applicationEntity = new ApplicationEntity();
    when(repo.findById(applicationId)).thenReturn(Optional.of(applicationEntity));
    when(declarationMapper.toDeclarationEntity(declarationCommand)).thenReturn(declarationEntity);

    // Act
    final boolean result = sut.updateClientDeclaration(applicationId, declarationCommand);

    // Assert
    assertTrue(result);
    assertThat(applicationEntity.getDeclaration()).isNotNull();
    assertThat(applicationEntity.getDeclaration().isDeclarationConfirmation()).isTrue();
    assertThat(applicationEntity.getDeclaration().getClientDeclarationStatus())
        .isEqualTo(ClientDeclarationStatus.DRAFT);
    assertThat(applicationEntity.getCreatedBy()).isEqualTo(userContext.getCurrentUser());
    assertThat(applicationEntity.getModifiedBy()).isEqualTo(userContext.getCurrentUser());
    verify(repo, times(1)).save(applicationEntity);
  }

  @Test
  void updateDeclaration_shouldUpdateDeclaration_whenApplicationHasExistingDeclaration() {
    // Arrange
    final UUID originalDeclarationId = UUID.randomUUID();
    final String originalUser = "Original creator";
    Instant originalCreatedTime = Instant.now().minusSeconds(3600);
    final UUID applicationId = UUID.randomUUID();
    final DeclarationCommand declarationCommand =
        DeclarationCommand.builder().declarationConfirmation(true).build();
    final DeclarationEntity declarationEntity =
        DeclarationEntityGenerator.createWithoutId(
            builder -> builder.declarationConfirmation(true).clientDeclarationStatus(null));
    when(declarationMapper.toDeclarationEntity(declarationCommand)).thenReturn(declarationEntity);
    final ApplicationEntity applicationEntity =
        ApplicationEntityGenerator.createWithId(
            builder -> {
              builder.declaration(
                  DeclarationEntityGenerator.createWithoutId(
                      declarationBuilder -> {
                        declarationBuilder
                            .declarationConfirmation(false)
                            .clientDeclarationStatus(null)
                            .id(originalDeclarationId)
                            .createdAt(originalCreatedTime)
                            .createdBy(originalUser)
                            .modifiedAt(originalCreatedTime)
                            .modifiedBy(originalUser);
                      }));
              builder.createdAt(originalCreatedTime);
              builder.createdBy(originalUser);
              builder.modifiedAt(originalCreatedTime);
              builder.modifiedBy(originalUser);
            });
    when(repo.findById(applicationId)).thenReturn(Optional.of(applicationEntity));

    assertThat(applicationEntity.getDeclaration().getCreatedAt()).isEqualTo(originalCreatedTime);
    assertThat(applicationEntity.getDeclaration().getModifiedAt()).isEqualTo(originalCreatedTime);

    // Act
    final boolean result = sut.updateClientDeclaration(applicationId, declarationCommand);
    final DeclarationEntity declaration = applicationEntity.getDeclaration();

    // Assert
    assertTrue(result);
    assertThat(declaration).isNotNull();
    assertThat(declaration.isDeclarationConfirmation()).isTrue();
    assertThat(declaration.getClientDeclarationStatus()).isEqualTo(ClientDeclarationStatus.DRAFT);

    assertThat(declaration.getId()).isEqualTo(originalDeclarationId);
    assertThat(declaration.getCreatedBy()).isEqualTo(originalUser);
    assertThat(declaration.getCreatedAt()).isEqualTo(originalCreatedTime);

    assertThat(declaration.getModifiedAt()).isNotEqualTo(originalCreatedTime);
    assertThat(declaration.getModifiedBy()).isEqualTo(userContext.getCurrentUser());

    assertThat(applicationEntity.getModifiedAt()).isNotEqualTo(originalCreatedTime);
    assertThat(applicationEntity.getModifiedBy()).isEqualTo(userContext.getCurrentUser());

    verify(repo, times(1)).save(applicationEntity);
    verify(declarationMapper, times(1)).toDeclarationEntity(declarationCommand);
  }

  @Test
  void updateDeclaration_shouldReturnFalse_whenApplicationDoesNotExist() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    final boolean result = sut.updateClientDeclaration(UUID.randomUUID(), null);

    // Assert
    assertFalse(result);
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void updateEvidence_shouldReturnFalse_whenApplicationDoesNotExist() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    final boolean result = sut.updateEvidence(UUID.randomUUID(), null);

    // Assert
    assertFalse(result);
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void updateEvidence_whenNoEvidenceOnApplication_shouldCreateNewEvidence() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final ApplicationEntity applicationEntity =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.withDefaultClientDetails().evidence(null));
    when(repo.findById(applicationId)).thenReturn(Optional.of(applicationEntity));

    // Act
    final boolean result =
        sut.updateEvidence(
            applicationId,
            UpdateEvidenceGenerator.createUpdateEvidenceCommand(
                builder -> {
                  builder
                      .capitalEvidence(true)
                      .housingCostsEvidence(true)
                      .payeIncomeEvidence(true)
                      .otherIncomeEvidence(true);
                }));

    // Assert
    assertTrue(result);
    assertThat(applicationEntity.getEvidence()).isNotNull();
    assertTrue(applicationEntity.getEvidence().isCapitalEvidence());
    assertTrue(applicationEntity.getEvidence().isHousingCostsEvidence());
    assertTrue(applicationEntity.getEvidence().isPayeIncomeEvidence());
    assertTrue(applicationEntity.getEvidence().isOtherIncomeEvidence());
    assertThat(applicationEntity.getEvidence().getModifiedAt()).isNotNull();
    assertThat(applicationEntity.getEvidence().getModifiedBy())
        .isEqualTo(userContext.getCurrentUser());
    assertThat(applicationEntity.getEvidence().getCreatedBy())
        .isEqualTo(userContext.getCurrentUser());
    assertThat(applicationEntity.getModifiedBy()).isEqualTo(userContext.getCurrentUser());
    verify(repo, times(1)).save(applicationEntity);
  }

  @Test
  void updateEvidence_whenEvidenceExistsOnApplication_shouldUpdateExistingEvidence() {
    final Instant originalCreatedTime = Instant.now().minusSeconds(3600);
    final String originalCreator = "Original creator";
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final UUID evidenceId = UUID.randomUUID();
    final ApplicationEntity applicationEntity =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .id(applicationId)
                    .createdAt(originalCreatedTime)
                    .createdBy(originalCreator)
                    .modifiedAt(originalCreatedTime)
                    .modifiedBy(originalCreator)
                    .evidence(
                        EvidenceEntityGenerator.createWithoutId(
                            evidenceBuilder -> {
                              evidenceBuilder
                                  .id(evidenceId)
                                  .capitalEvidence(true)
                                  .housingCostsEvidence(true)
                                  .payeIncomeEvidence(true)
                                  .otherIncomeEvidence(true)
                                  .createdBy(originalCreator)
                                  .createdAt(originalCreatedTime)
                                  .modifiedAt(originalCreatedTime)
                                  .modifiedBy(originalCreator);
                            })));
    final EvidenceEntity existingEvidence = applicationEntity.getEvidence();
    when(repo.findById(applicationId)).thenReturn(Optional.of(applicationEntity));

    // Act
    final boolean result =
        sut.updateEvidence(
            applicationId,
            UpdateEvidenceGenerator.createUpdateEvidenceCommand(
                builder -> {
                  builder
                      .capitalEvidence(false)
                      .housingCostsEvidence(false)
                      .payeIncomeEvidence(false)
                      .otherIncomeEvidence(false);
                }));

    // Assert
    assertTrue(result);
    assertThat(applicationEntity.getEvidence()).isNotNull();

    assertSame(existingEvidence, applicationEntity.getEvidence());
    assertFalse(applicationEntity.getEvidence().isCapitalEvidence());
    assertFalse(applicationEntity.getEvidence().isHousingCostsEvidence());
    assertFalse(applicationEntity.getEvidence().isPayeIncomeEvidence());
    assertFalse(applicationEntity.getEvidence().isOtherIncomeEvidence());

    assertThat(applicationEntity.getEvidence().getCreatedAt()).isEqualTo(originalCreatedTime);
    assertThat(applicationEntity.getEvidence().getCreatedBy()).isEqualTo(originalCreator);
    assertThat(applicationEntity.getEvidence().getModifiedAt()).isNotEqualTo(originalCreatedTime);
    assertThat(applicationEntity.getEvidence().getModifiedBy())
        .isEqualTo(userContext.getCurrentUser());

    assertThat(applicationEntity.getModifiedAt()).isNotEqualTo(originalCreatedTime);
    assertThat(applicationEntity.getModifiedBy()).isEqualTo(userContext.getCurrentUser());
    assertThat(applicationEntity.getCreatedAt()).isEqualTo(originalCreatedTime);

    verify(repo, times(1)).save(applicationEntity);
  }
}
