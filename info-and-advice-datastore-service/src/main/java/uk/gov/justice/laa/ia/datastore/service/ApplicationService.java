package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
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
import uk.gov.justice.laa.ia.datastore.exception.EtagMismatchException;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.mapper.DeclarationMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.ApplicationSummary;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;

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
  private final EventService eventService;

  /**
   * Create an application.
   *
   * @return ID of newly created application.
   */
  @Transactional
  public UUID createApplication(StartCaseCommand startCase) {
    ApplicationEntity entity = applicationMapper.toApplicationEntity(startCase);

    entity.setApplicationState(ApplicationState.DRAFT);
    ApplicationEntity saved = repository.save(entity);
    eventService.record(startCase);
    return saved.getId();
  }

  /**
   * Gets all the Applications.
   *
   * @param additionalFilteringSpecification the specification to filter applications, if null will
   *     just filter to the provider firm ID.
   * @param page the page number to retrieve, if null will use default page 0.
   * @param size the page size, if null will use default page size.
   * @return page of {@link ApplicationSummary}
   */
  public Page<ApplicationSummary> getAllApplications(
      Specification<ApplicationEntity> additionalFilteringSpecification,
      Integer page,
      Integer size) {

    int resolvedPage = page != null ? page : 0;
    int resolvedSize = size != null ? size : DEFAULT_PAGE_SIZE;

    Specification<ApplicationEntity> specificationToApply =
        ApplicationSpecification.filterByProviderFirmId(userContext.getProviderFirmId());
    if (additionalFilteringSpecification != null) {
      specificationToApply = specificationToApply.and(additionalFilteringSpecification);
    }

    return repository
        .findAll(specificationToApply, PageRequest.of(resolvedPage, resolvedSize))
        .map(applicationMapper::toApplicationSummary);
  }

  /**
   * Gets an Application or empty optional if not found.
   *
   * @return {@link ApplicationResponse}
   */
  public Optional<ApplicationResponse> getApplication(UUID applicationId) {
    var findApplicationByIdSpecification =
        ApplicationSpecification.findById(applicationId, userContext.getProviderFirmId());
    return repository
        .findOne(findApplicationByIdSpecification)
        .map(applicationMapper::toApplication);
  }

  /**
   * Update means data for an application.
   *
   * @param applicationId the application ID
   * @param version the expected ETag version
   * @param body the means data
   * @return true if application updated, false if not found
   * @throws EtagMismatchException if the provided version does not match the current entity version
   */
  @Transactional
  public boolean updateMeansData(UUID applicationId, long version, Object body) {
    Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmId()));
    if (applicationOpt.isEmpty()) {
      return false;
    }

    ApplicationEntity application = applicationOpt.get();
    if (application.getVersion() != version) {
      throw new EtagMismatchException();
    }

    JsonNode jsonNode = objectMapper.valueToTree(body);

    // Save the result to the new table
    EligibilityResultEntity resultEntity =
        EligibilityResultEntity.builder()
            .applicationId(applicationId)
            .resultJson(jsonNode)
            .createdBy(userContext.getCurrentUser())
            .build();

    eligibilityResultRepository.save(resultEntity);

    // Update application fields if present in the JSON
    if (jsonNode.has("determinationId")) {
      application.setDeterminationId(UUID.fromString(jsonNode.get("determinationId").asText()));
    }
    if (jsonNode.has("meansAssessmentRequired")) {
      application.setMeansAssessmentRequired(jsonNode.get("meansAssessmentRequired").asBoolean());
    }

    application.setModifiedBy(userContext.getCurrentUser());
    repository.save(application);
    eventService.record(body);
    return true;
  }

  /**
   * Update application client declaration..
   *
   * @param version the expected ETag version
   * @return true if application updated, false if not found.
   * @throws EtagMismatchException if the provided version does not match the current entity version
   */
  @Transactional
  public boolean updateClientDeclaration(
      UUID applicationId, long version, DeclarationCommand declarationConfirmation) {
    final Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmId()));
    if (applicationOpt.isEmpty()) {
      return false;
    }

    final ApplicationEntity application = applicationOpt.get();
    if (application.getVersion() != version) {
      throw new EtagMismatchException();
    }

    DeclarationEntity declarationEntity =
        declarationMapper.toDeclarationEntity(declarationConfirmation);
    // TODO: declaration status is currently undefined
    declarationEntity.setClientDeclarationStatus(ClientDeclarationStatus.DRAFT);

    if (application.getDeclaration() != null) {
      declarationEntity.setId(application.getDeclaration().getId());
      declarationEntity.setCreatedAt(application.getDeclaration().getCreatedAt());
      declarationEntity.setCreatedBy(application.getDeclaration().getCreatedBy());
    }
    application.setDeclaration(declarationEntity);

    repository.save(application);
    eventService.record(declarationConfirmation);
    return true;
  }

  /**
   * Update application evidence.
   *
   * @param version the expected ETag version
   * @return true if application updated, false if not found.
   * @throws EtagMismatchException if the provided version does not match the current entity version
   */
  @Transactional
  public boolean updateEvidence(UUID applicationId, long version, Map<String, Object> evidence) {

    final Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmId()));
    if (applicationOpt.isEmpty()) {
      return false;
    }
    final ApplicationEntity application = applicationOpt.get();
    if (application.getVersion() != version) {
      throw new EtagMismatchException();
    }
    application.setModifiedBy(userContext.getCurrentUser());
    application.setEvidence(evidence);
    repository.save(application);
    eventService.record(evidence);
    return true;
  }
}
