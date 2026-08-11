package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
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
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.exception.EtagMismatchException;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceGenerator;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.mapper.DeclarationMapper;
import uk.gov.justice.laa.ia.datastore.mapper.EvidenceMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.ApplicationSummary;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateMeansDataCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;
import uk.gov.justice.laa.ia.datastore.repository.EvidenceRepository;

/** Unit tests for the {@link ApplicationService}. */
@ExtendWith(MockitoExtension.class)
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ApplicationServiceTest {
  @Mock private ApplicationRepository repo;
  @Mock private EligibilityResultRepository eligibilityResultRepository;
  @Mock private EvidenceRepository evidenceRepository;
  @Mock private ApplicationMapper mapper;
  @Mock private DeclarationMapper declarationMapper;
  @Mock private EvidenceMapper evidenceMapper;
  @Mock private UserContext userContext;
  @Mock private ObjectMapper objectMapper;
  @Mock private EventService eventService;
  @Mock private EntityManager entityManager;

  @InjectMocks private ApplicationService sut;

  @Test
  void shouldCreateApplication() {
    // Arrange
    final UUID officeId = UUID.randomUUID();
    final StartApplicationCommand cmd =
        StartApplicationCommand.builder().providerOfficeCode(officeId.toString()).build();
    final ApplicationEntity entity = new ApplicationEntity();
    entity.setProviderOfficeCode(officeId.toString());
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
    ApplicationResponse expectedResponse = new ApplicationResponse();
    expectedResponse.setId(generatedId);
    when(mapper.toApplication(entity)).thenReturn(expectedResponse);

    // Act
    final ApplicationResponse result = sut.createApplication(cmd);

    // Assert
    assertThat(result.getId()).isEqualTo(generatedId);
    assertThat(entity.getProviderOfficeCode()).isEqualTo(officeId.toString());
    assertThat(entity.getApplicationState()).isEqualTo(ApplicationState.DRAFT);

    verify(repo, times(1)).save(entity);
  }

  @Test
  void shouldGetAllApplications() {
    // Arrange
    final ApplicationEntity entity1 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationEntity entity2 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationSummary summary1 = new ApplicationSummary(entity1.getId(), "REF-001", null);
    final ApplicationSummary summary2 = new ApplicationSummary(entity2.getId(), "REF-002", null);

    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entity1, entity2)));
    when(mapper.toApplicationSummary(entity1)).thenReturn(summary1);
    when(mapper.toApplicationSummary(entity2)).thenReturn(summary2);

    // Act
    final Page<ApplicationSummary> result = sut.getAllApplications(null, null, null);

    // Assert
    assertThat(result.getContent()).hasSize(2).contains(summary1, summary2);
    assertThat(result.getTotalElements()).isEqualTo(2);
    verify(repo, times(1)).findAll(any(Specification.class), any(Pageable.class));
    verify(mapper, times(2)).toApplicationSummary(any());
  }

  @Test
  void shouldPassSpecificationToRepository_whenGettingAllApplications() {
    // Arrange
    final ApplicationEntity entity1 = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationSummary summary1 = new ApplicationSummary(entity1.getId(), "REF-001", null);
    when(repo.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(entity1)));
    when(mapper.toApplicationSummary(entity1)).thenReturn(summary1);
    when(userContext.getProviderFirmCode()).thenReturn("123456");

    // Act
    final Page<ApplicationSummary> result =
        sut.getAllApplications(
            (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("id"), entity1.getId()),
            0,
            10);

    // Assert
    assertThat(result.getContent()).hasSize(1).contains(summary1);
    verify(repo, times(1)).findAll(any(Specification.class), any(Pageable.class));
    verify(mapper, times(1)).toApplicationSummary(any());
  }

  @Test
  void shouldGetApplication() {
    // Arrange
    final ApplicationEntity entity = ApplicationEntity.builder().id(UUID.randomUUID()).build();
    final ApplicationResponse application =
        ApplicationResponse.builder().id(entity.getId()).build();
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(entity));

    when(mapper.toApplication(entity)).thenReturn(application);

    // Act
    final ApplicationResponse result = sut.getApplication(entity.getId()).orElseThrow();

    // Assert
    assertThat(result).isEqualTo(application);
    verify(repo, times(1)).findOne(any(Specification.class));
    verify(mapper, times(1)).toApplication(entity);
  }

  @Test
  void shouldReturnEmptyOptional_whenApplicationDoesNotExist() {
    // Arrange
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.empty());

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
    final Object data = new ObjectMapper().createObjectNode().put("question", "answer");
    final Object meansResult = new ObjectMapper().createObjectNode().put("status", "ELIGIBLE");
    final UpdateMeansDataCommand command =
        UpdateMeansDataCommand.builder().eTag(0L).data(data).result(meansResult).build();
    final ObjectNode dataNode = new ObjectMapper().createObjectNode().put("question", "answer");
    final ObjectNode resultNode = new ObjectMapper().createObjectNode().put("status", "ELIGIBLE");
    final String user = "TEST_USER";

    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));
    when(userContext.getCurrentUser()).thenReturn(user);
    when(objectMapper.valueToTree(data)).thenReturn(dataNode);
    when(objectMapper.valueToTree(meansResult)).thenReturn(resultNode);

    // Act
    boolean result = sut.updateMeansData(applicationId, command);
    verify(repo, times(1)).save(application);
    assertThat(application.getModifiedBy()).isEqualTo(user);

    ArgumentCaptor<EligibilityResultEntity> resultCaptor =
        ArgumentCaptor.forClass(EligibilityResultEntity.class);
    verify(eligibilityResultRepository).save(resultCaptor.capture());
    assertThat(resultCaptor.getValue().getApplicationId()).isEqualTo(applicationId);
    assertThat(resultCaptor.getValue().getData()).isEqualTo(dataNode);
    assertThat(resultCaptor.getValue().getResultJson()).isEqualTo(resultNode);
  }

  @Test
  void shouldReturnFalse_whenUpdatingMeansDataForUnknownApplication() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.empty());

    // Act
    boolean result =
        sut.updateMeansData(applicationId, UpdateMeansDataCommand.builder().eTag(0L).build());
    verify(eligibilityResultRepository, never()).save(any(EligibilityResultEntity.class));
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void updateDeclaration_shouldCreateDeclaration_whenApplicationHasNoDeclaration() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final DeclarationCommand declarationCommand =
        DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build();
    final DeclarationEntity declarationEntity =
        DeclarationEntityGenerator.createWithId(builder -> builder.declarationConfirmation(true));
    final ApplicationEntity applicationEntity = new ApplicationEntity();
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(applicationEntity));
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
        DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build();
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
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(applicationEntity));

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

    verify(repo, times(1)).save(applicationEntity);
    verify(declarationMapper, times(1)).toDeclarationEntity(declarationCommand);
  }

  @Test
  void updateDeclaration_shouldReturnFalse_whenApplicationDoesNotExist() {
    // Arrange
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.empty());

    // Act
    final boolean result =
        sut.updateClientDeclaration(UUID.randomUUID(), (DeclarationCommand) null);

    // Assert
    assertFalse(result);
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void updateEvidence_shouldReturnFalse_whenApplicationDoesNotExist() {
    // Arrange
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.empty());

    // Act
    final boolean result = sut.updateEvidence(UUID.randomUUID(), (UpdateEvidenceCommand) null);

    // Assert
    assertFalse(result);
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void updateEvidence_shouldUpdateEvidence_whenApplicationExistsAndEvidenceDoesNotExist() {
    // Arrange
    final var command = EvidenceGenerator.createUpdateEvidenceCommand(0L);
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.evidence(null));
    final EvidenceEntity savedEvidence =
        EvidenceEntity.builder().evidenceId(UUID.randomUUID()).build();

    when(userContext.getCurrentUser()).thenReturn("TEST_USER");
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));
    when(evidenceMapper.toEvidenceEntity(command)).thenReturn(EvidenceEntity.builder().build());
    when(evidenceRepository.save(any(EvidenceEntity.class))).thenReturn(savedEvidence);

    // Act
    final boolean result = sut.updateEvidence(application.getId(), command);

    // Assert
    assertTrue(result);
    assertThat(application.getEvidence()).isSameAs(savedEvidence);
    assertThat(application.getModifiedBy()).isEqualTo("TEST_USER");
    verify(evidenceRepository, times(1)).save(any(EvidenceEntity.class));
    verify(repo, times(1)).save(application);
  }

  @Test
  void updateEvidence_shouldUpdateEvidence_whenApplicationExistsAndEvidenceExists() {
    // Arrange
    final UUID existingEvidenceId = UUID.randomUUID();
    final EvidenceEntity existingEvidence =
        EvidenceEntity.builder().evidenceId(existingEvidenceId).createdBy("ORIGINAL_USER").build();
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.evidence(existingEvidence));
    final var command = EvidenceGenerator.createUpdateEvidenceCommand(0L);
    final EvidenceEntity mappedEvidence = EvidenceEntity.builder().build();
    final EvidenceEntity savedEvidence =
        EvidenceEntity.builder().evidenceId(existingEvidenceId).build();

    when(userContext.getCurrentUser()).thenReturn("TEST_USER");
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));
    when(evidenceMapper.toEvidenceEntity(command)).thenReturn(mappedEvidence);
    when(evidenceRepository.save(any(EvidenceEntity.class))).thenReturn(savedEvidence);

    // Act
    final boolean result = sut.updateEvidence(application.getId(), command);

    // Assert
    assertTrue(result);
    assertThat(application.getEvidence()).isSameAs(savedEvidence);
    assertThat(mappedEvidence.getEvidenceId()).isEqualTo(existingEvidenceId);
    verify(evidenceRepository, times(1)).save(mappedEvidence);
    verify(repo, times(1)).save(application);
  }

  @Test
  void shouldRecordEvent_whenApplicationCreated() {
    // Arrange
    final StartApplicationCommand cmd = StartApplicationCommand.builder().build();
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
    final Object data = new ObjectMapper().createObjectNode().put("question", "answer");
    final Object meansResult = new ObjectMapper().createObjectNode().put("status", "ELIGIBLE");
    final UpdateMeansDataCommand command =
        UpdateMeansDataCommand.builder().eTag(0L).data(data).result(meansResult).build();
    final ObjectNode objectNode = new ObjectMapper().createObjectNode();
    final ApplicationEntity application = ApplicationEntity.builder().id(applicationId).build();
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));
    when(objectMapper.valueToTree(data)).thenReturn(objectNode);
    when(objectMapper.valueToTree(meansResult)).thenReturn(objectNode);

    // Act
    sut.updateMeansData(applicationId, command);
  }

  @Test
  void shouldNotRecordEvent_whenMeansDataUpdatedForUnknownApplication() {
    // Arrange
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.empty());

    // Act
    sut.updateMeansData(UUID.randomUUID(), UpdateMeansDataCommand.builder().eTag(0L).build());

    // Assert
    verify(eventService, never()).record(any());
  }

  @Test
  void shouldRecordEvent_whenDeclarationUpdated() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final DeclarationCommand command =
        DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build();
    final DeclarationEntity declarationEntity =
        DeclarationEntityGenerator.createWithId(builder -> builder.declarationConfirmation(true));
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(new ApplicationEntity()));
    when(declarationMapper.toDeclarationEntity(command)).thenReturn(declarationEntity);

    // Act
    sut.updateClientDeclaration(applicationId, command);

    // Assert
    verify(eventService, times(1)).record(command);
  }

  @Test
  void shouldNotRecordEvent_whenDeclarationUpdatedForUnknownApplication() {
    // Arrange
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.empty());

    // Act
    sut.updateClientDeclaration(UUID.randomUUID(), (DeclarationCommand) null);

    // Assert
    verify(eventService, never()).record(any());
  }

  @Test
  void shouldRecordEvent_whenEvidenceUpdated() {
    // Arrange
    final var command = EvidenceGenerator.createUpdateEvidenceCommand(0L);
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.evidence(null));
    when(userContext.getCurrentUser()).thenReturn("TEST_USER");
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));
    when(evidenceMapper.toEvidenceEntity(command)).thenReturn(EvidenceEntity.builder().build());
    when(evidenceRepository.save(any(EvidenceEntity.class)))
        .thenReturn(EvidenceEntity.builder().build());

    // Act
    sut.updateEvidence(application.getId(), command);

    // Assert
    verify(eventService, times(1)).record(command);
  }

  @Test
  void shouldNotRecordEvent_whenEvidenceUpdatedForUnknownApplication() {
    // Arrange
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.empty());

    // Act
    sut.updateEvidence(UUID.randomUUID(), (UpdateEvidenceCommand) null);

    // Assert
    verify(eventService, never()).record(any());
  }

  @Test
  void updateMeansData_shouldThrowEtagMismatchException_whenVersionDoesNotMatch() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final ApplicationEntity application =
        ApplicationEntity.builder().id(applicationId).build(); // eTag = 0
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));

    // Act + Assert
    assertThrows(
        EtagMismatchException.class,
        () ->
            sut.updateMeansData(applicationId, UpdateMeansDataCommand.builder().eTag(99L).build()));
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void updateClientDeclaration_shouldThrowEtagMismatchException_whenVersionDoesNotMatch() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final ApplicationEntity application = new ApplicationEntity(); // eTag = 0
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));

    // Act + Assert
    assertThrows(
        EtagMismatchException.class,
        () ->
            sut.updateClientDeclaration(
                applicationId, DeclarationCommand.builder().eTag(99L).build()));
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void updateEvidence_shouldThrowEtagMismatchException_whenVersionDoesNotMatch() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(builder -> builder.evidence(null)); // eTag = 0
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));

    // Act + Assert
    assertThrows(
        EtagMismatchException.class,
        () ->
            sut.updateEvidence(
                application.getId(), UpdateEvidenceCommand.builder().eTag(99L).build()));
    verify(repo, never()).save(any(ApplicationEntity.class));
  }

  @Test
  void shouldUpdateApplication() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final ApplicationEntity application =
        ApplicationEntity.builder().id(applicationId).build(); // eTag = 0
    final UpdateApplicationCommand command =
        UpdateApplicationCommand.builder()
            .eTag(0L)
            .applicationState(ApplicationState.COMPLETED)
            .build();
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));

    // Act
    boolean result = sut.updateApplication(applicationId, command);

    // Assert
    assertTrue(result);
    verify(mapper, times(1)).updateApplicationEntity(command, application);
    verify(repo, times(1)).save(application);
    verify(eventService, times(1)).record(command);
  }

  @Test
  void shouldReturnFalse_whenUpdatingApplicationForUnknownApplication() {
    // Arrange
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.empty());

    // Act
    boolean result =
        sut.updateApplication(
            UUID.randomUUID(), UpdateApplicationCommand.builder().eTag(0L).build());

    // Assert
    assertFalse(result);
    verify(mapper, never()).updateApplicationEntity(any(), any());
    verify(repo, never()).save(any(ApplicationEntity.class));
    verify(eventService, never()).record(any());
  }

  @Test
  void updateApplication_shouldThrowEtagMismatchException_whenVersionDoesNotMatch() {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final ApplicationEntity application =
        ApplicationEntity.builder().id(applicationId).build(); // eTag = 0
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(repo.findOne(any(Specification.class))).thenReturn(Optional.of(application));

    // Act + Assert
    assertThrows(
        EtagMismatchException.class,
        () ->
            sut.updateApplication(
                applicationId, UpdateApplicationCommand.builder().eTag(99L).build()));
    verify(repo, never()).save(any(ApplicationEntity.class));
  }
}
