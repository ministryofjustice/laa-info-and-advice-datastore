package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
    entity.setApplicationState(ApplicationState.DRAFT);
    entity.setCreatedBy(userContext.getCurrentUser());
    entity.setModifiedBy(userContext.getCurrentUser());

    ApplicationEntity saved = repository.save(entity);
    return saved.getId();
  }

  /**
   * Gets all the Applications.
   *
   * @param specification the specification to filter applications, if null will use unrestricted
   *     specification.
   * @param page the page number to retrieve, if null will use default page 0.
   * @param size the page size, if null will use default page size.
   * @return page of {@link ApplicationResponse}
   */
  public Page<ApplicationResponse> getAllApplications(
      Specification<ApplicationEntity> specification, Integer page, Integer size) {

    int resolvedPage = page != null ? page : 0;
    int resolvedSize = size != null ? size : DEFAULT_PAGE_SIZE;
    Specification<ApplicationEntity> resolvedSpecification =
        specification != null ? specification : Specification.unrestricted();

    return repository
        .findAll(resolvedSpecification, PageRequest.of(resolvedPage, resolvedSize))
        .map(applicationMapper::toApplication);
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
    if (jsonNode.has("determinationId")) {
      application.setDeterminationId(UUID.fromString(jsonNode.get("determinationId").asText()));
    }
    if (jsonNode.has("meansAssessmentRequired")) {
      application.setMeansAssessmentRequired(jsonNode.get("meansAssessmentRequired").asBoolean());
    }

    application.setModifiedAt(Instant.now());
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

  /**
   * Update application evidence.
   *
   * @return true if application updated, false if not found.
   */
  public boolean updateEvidence(UUID applicationId, Map<String, Object> evidence) {

    final Optional<ApplicationEntity> applicationOpt = repository.findById(applicationId);
    if (applicationOpt.isEmpty()) {
      return false;
    }
    final ApplicationEntity application = applicationOpt.get();
    application.setModifiedBy(userContext.getCurrentUser());
    application.setModifiedAt(Instant.now());
    application.setEvidence(evidence);
    repository.save(application);
    return true;
  }
}
