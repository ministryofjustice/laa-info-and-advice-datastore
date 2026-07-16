package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceGenerator;
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
  @Mock private EventService eventService;

  @InjectMocks private ApplicationService sut;

  @Test
  void shouldCreateApplication() {
    // Arrange
    final UUID officeId = UUID.randomUUID();
    final StartCaseCommand cmd = StartCaseCommand.builder().providerOfficeId(officeId).build();
    final ApplicationEntity entity = new ApplicationEntity();
    entity.setProviderOfficeId(officeId);
    entity.setClientDetails(ClientDetailsEntity.builder().build());
    final UUID generatedId = UUID.randomUUID();
    final String user = "TEST_USER";

    when(mapper.toApplicationEntity(cmd)).thenReturn(entity);
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
    assertThat(entity.getProviderOfficeId()).isEqualTo(officeId);
    assertThat(entity.getApplicationState()).isEqualTo(ApplicationState.DRAFT);

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
    when(repo.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entity1, entity2)));
    when(mapper.toApplication(entity1)).thenReturn(application1);
    when(mapper.toApplication(entity2)).thenReturn(application2);

    // Act
    final Page<ApplicationResponse> result = sut.getAllApplications(null, null, null);

    // Assert
    assertThat(result.getContent()).hasSize(2).contains(application1, application2);
    assertThat(result.getTotalElements()).isEqualTo(2);
    verify(repo, times(1)).findAll(any(Specification.class), any(Pageable.class));
    verify(mapper, times(2)).toApplication(any());
  }

  @Test
  void shouldPassSpecificationToRepository_whenGettingAllApplications() {
    // Arrange
    final ApplicationEntity entity1 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationResponse application1 =
        ApplicationResponse.builder().id(entity1.getId()).build();
    when(repo.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entity1)));
    when(mapper.toApplication(entity1)).thenReturn(application1);

    // Act
    final Page<ApplicationResponse> result =
        sut.getAllApplications(
            (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id"), entity1.getId()),
            0,
            10);

    // Assert
    assertThat(result.getContent()).hasSize(1).contains(application1);
    verify(repo, times(1)).findAll(any(Specification.class), any(Pageable.class));
    verify(mapper, times(1)).toApplication(any());
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
    final ApplicationEntity application =
        ApplicationEntity.builder().id(applicationId).modifiedAt(originalModifiedAt).build();
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

    ArgumentCaptor<EligibilityResultEntity> resultCaptor =
        ArgumentCaptor.forClass(EligibilityResultEntity.class);
    verify(eligibilityResultRepository).save(resultCaptor.capture());
    assertThat(resultCaptor.getValue().getApplicationId()).isEqualTo(applicationId);
    assertThat(resultCaptor.getValue().getResultJson()).isEqualTo(jsonNode);
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
            builder ->
                builder
                    .declarationConfirmation(true)
                    .clientDeclarationStatus(null)
                    .modifiedBy(originalUser)
                    .createdBy(originalUser));
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
  void updateEvidence_shouldUpdateEvidence_whenApplicationExistsAndEvidenceDoesNotExist() {
    // Arrange
    final var evidence = EvidenceGenerator.createEvidenceMap();
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.evidence(null));

    when(userContext.getCurrentUser()).thenReturn("TEST_USER");
    when(repo.findById(application.getId())).thenReturn(Optional.of(application));

    // Act
    final boolean result = sut.updateEvidence(application.getId(), evidence);

    // Assert
    assertTrue(result);
    assertThat(application.getEvidence()).isSameAs(evidence);
    assertThat(application.getModifiedBy()).isEqualTo(userContext.getCurrentUser());
    assertThat(application.getModifiedAt()).isNotNull();
    verify(repo, times(1)).save(application);
  }

  @Test
  void updateEvidence_shouldUpdateEvidence_whenApplicationExistsAndEvidenceExists() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> builder.evidence(EvidenceGenerator.createEvidenceMap()));
    final var originalEvidence = application.getEvidence();
    final var evidence = EvidenceGenerator.createEvidenceMap();
    when(userContext.getCurrentUser()).thenReturn("TEST_USER");
    when(repo.findById(application.getId())).thenReturn(Optional.of(application));

    // Act
    final boolean result = sut.updateEvidence(application.getId(), evidence);

    // Assert
    assertTrue(result);
    assertThat(application.getEvidence()).isSameAs(evidence);
    assertThat(application.getEvidence()).isNotSameAs(originalEvidence);
    assertThat(application.getModifiedBy()).isEqualTo(userContext.getCurrentUser());
    assertThat(application.getModifiedAt()).isNotNull();
    verify(repo, times(1)).save(application);
  }

  @Test
  void shouldRecordEvent_whenApplicationCreated() {
    // Arrange
    final StartCaseCommand cmd = StartCaseCommand.builder().build();
    final ApplicationEntity entity = new ApplicationEntity();
    when(mapper.toApplicationEntity(cmd)).thenReturn(entity);
    when(repo.save(any(ApplicationEntity.class))).thenAnswer(i -> i.getArgument(0));

    // Act
    sut.createApplication(cmd);

    // Assert
    verify(eventService, times(1)).record(cmd);
  }

  @Test
  void shouldRecordEvent_whenMeansDataUpdated() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final Object body = new Object();
    final JsonNode jsonNode = new ObjectMapper().createObjectNode();
    final ApplicationEntity application = ApplicationEntity.builder().id(applicationId).build();
    when(repo.findById(applicationId)).thenReturn(Optional.of(application));
    when(userContext.canAccessApplication(application)).thenReturn(true);
    when(objectMapper.valueToTree(body)).thenReturn(jsonNode);

    // Act
    sut.updateMeansData(applicationId, body);

    // Assert
    verify(eventService, times(1)).record(body);
  }

  @Test
  void shouldNotRecordEvent_whenMeansDataUpdatedForUnknownApplication() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    sut.updateMeansData(UUID.randomUUID(), new Object());

    // Assert
    verify(eventService, never()).record(any());
  }

  @Test
  void shouldRecordEvent_whenDeclarationUpdated() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final DeclarationCommand command =
        DeclarationCommand.builder().declarationConfirmation(true).build();
    final DeclarationEntity declarationEntity =
        DeclarationEntityGenerator.createWithId(builder -> builder.declarationConfirmation(true));
    when(repo.findById(applicationId)).thenReturn(Optional.of(new ApplicationEntity()));
    when(declarationMapper.toDeclarationEntity(command)).thenReturn(declarationEntity);

    // Act
    sut.updateClientDeclaration(applicationId, command);

    // Assert
    verify(eventService, times(1)).record(command);
  }

  @Test
  void shouldNotRecordEvent_whenDeclarationUpdatedForUnknownApplication() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    sut.updateClientDeclaration(UUID.randomUUID(), null);

    // Assert
    verify(eventService, never()).record(any());
  }

  @Test
  void shouldRecordEvent_whenEvidenceUpdated() {
    // Arrange
    final var evidence = EvidenceGenerator.createEvidenceMap();
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.evidence(null));
    when(repo.findById(application.getId())).thenReturn(Optional.of(application));

    // Act
    sut.updateEvidence(application.getId(), evidence);

    // Assert
    verify(eventService, times(1)).record(evidence);
  }

  @Test
  void shouldNotRecordEvent_whenEvidenceUpdatedForUnknownApplication() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    sut.updateEvidence(UUID.randomUUID(), null);

    // Assert
    verify(eventService, never()).record(any());
  }
}
