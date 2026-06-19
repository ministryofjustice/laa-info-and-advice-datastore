package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.mapper.DeclarationMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;

/** Service class for handling Applications. */
@RequiredArgsConstructor
@Service
public class ApplicationService {

  private static final int DEFAULT_PAGE_SIZE = 25;

  private final ApplicationRepository repository;
  private final EligibilityResultRepository eligibilityResultRepository;
  private final ApplicationMapper applicationMapper;
  private final DeclarationMapper declarationMapper;
  private final UserContext userContext;
  private final ObjectMapper objectMapper;

  /**
   * Create an application.
   *
   * @return ID of newly created application.
   */
  @Transactional
  public UUID createApplication(StartCaseCommand startCase) {
    ApplicationEntity entity = applicationMapper.toApplicationEntity(startCase);

    // Set default/system values from context
    entity.setProviderFirmId(userContext.getProviderFirmId());
    entity.setProviderOfficeId(userContext.getProviderOfficeId());
    entity.setApplicationState(ApplicationState.DRAFT);
    entity.setCreatedBy(userContext.getCurrentUser());
    entity.setModifiedBy(userContext.getCurrentUser());

    ApplicationEntity saved = repository.save(entity);
    return saved.getId();
  }

  /**
   * Gets all the Applications.
   *
   * @return list of {@link ApplicationResponse}
   */
  public List<ApplicationResponse> getAllApplications(Integer page, Integer size) {

    int resolvedPage = page != null ? page : 0;
    int resolvedSize = size != null ? size : DEFAULT_PAGE_SIZE;

    return repository.findAll(PageRequest.of(resolvedPage, resolvedSize)).stream()
        .map(applicationMapper::toApplication)
        .toList();
  }

  /**
   * Gets an Application or empty optional if not found.
   *
   * @return {@link ApplicationResponse}
   */
  public Optional<ApplicationResponse> getApplication(UUID applicationId) {
    return repository
        .findById(applicationId)
        .filter(userContext::canAccessApplication)
        .map(applicationMapper::toApplication);
  }

  /**
   * Update means data for an application.
   *
   * @param applicationId the application ID
   * @param body the means data
   * @return true if application updated, false if not found
   */
  @Transactional
  public boolean updateMeansData(UUID applicationId, Object body) {
    Optional<ApplicationEntity> applicationOpt =
        repository.findById(applicationId).filter(userContext::canAccessApplication);
    if (applicationOpt.isEmpty()) {
      return false;
    }

    ApplicationEntity application = applicationOpt.get();

    JsonNode jsonNode = objectMapper.valueToTree(body);

    // Save the result to the new table
    EligibilityResultEntity resultEntity =
        EligibilityResultEntity.builder().applicationId(applicationId).resultJson(jsonNode).build();

    eligibilityResultRepository.save(resultEntity);

    // Update application fields if present in the JSON
    if (jsonNode.has("meansAssessmentId")) {
      application.setMeansAssessmentId(UUID.fromString(jsonNode.get("meansAssessmentId").asText()));
    }
    if (jsonNode.has("determinationId")) {
      application.setDeterminationId(UUID.fromString(jsonNode.get("determinationId").asText()));
    }
    if (jsonNode.has("meansAssessmentRequired")) {
      application.setMeansAssessmentRequired(jsonNode.get("meansAssessmentRequired").asBoolean());
    }

    application.setModifiedBy(userContext.getCurrentUser());
    repository.save(application);
    return true;
  }

  /**
   * Update application client declaration..
   *
   * @return true if application updated, false if not found.
   */
  public boolean updateClientDeclaration(
      UUID applicationId, DeclarationCommand declarationConfirmation) {
    final Optional<ApplicationEntity> applicationOpt = repository.findById(applicationId);
    if (applicationOpt.isEmpty()) {
      return false;
    }

    DeclarationEntity declarationEntity =
        declarationMapper.toDeclarationEntity(declarationConfirmation);
    // TODO: declaration status is currently undefined
    declarationEntity.setClientDeclarationStatus(ClientDeclarationStatus.DRAFT);
    Instant modifiedAt = Instant.now();

    ApplicationEntity application = applicationOpt.get();
    if (application.getDeclaration() != null) {
      declarationEntity.setId(application.getDeclaration().getId());
      declarationEntity.setCreatedAt(application.getDeclaration().getCreatedAt());
      declarationEntity.setCreatedBy(application.getDeclaration().getCreatedBy());
    } else {
      declarationEntity.setCreatedBy(userContext.getCurrentUser());
    }
    declarationEntity.setModifiedBy(userContext.getCurrentUser());
    declarationEntity.setModifiedAt(modifiedAt);
    application.setDeclaration(declarationEntity);
    application.setModifiedBy(userContext.getCurrentUser());
    application.setModifiedAt(modifiedAt);

    repository.save(application);
    return true;
  }
}
