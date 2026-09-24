package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.exception.DeclarationAlreadySignedException;
import uk.gov.justice.laa.ia.datastore.exception.DuplicateUfnException;
import uk.gov.justice.laa.ia.datastore.exception.EtagMismatchException;
import uk.gov.justice.laa.ia.datastore.exception.InvalidClientDetailsPatchException;
import uk.gov.justice.laa.ia.datastore.exception.ProviderOfficeNotAuthorizedException;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.mapper.ClientDetailsMapper;
import uk.gov.justice.laa.ia.datastore.mapper.DeclarationMapper;
import uk.gov.justice.laa.ia.datastore.mapper.EvidenceMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.ApplicationSummary;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.EditApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.PatchAddressData;
import uk.gov.justice.laa.ia.datastore.model.PatchClientDetailsData;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateClientDetailsCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateMeansDataCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateScopingDataCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;
import uk.gov.justice.laa.ia.datastore.repository.EvidenceRepository;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;

/** Service class for handling Applications. */
@RequiredArgsConstructor
@Service
public class ApplicationService {

  private final EntityManager entityManager;

  private static final int DEFAULT_PAGE_SIZE = 25;

  private final ApplicationRepository repository;
  private final EligibilityResultRepository eligibilityResultRepository;
  private final EvidenceRepository evidenceRepository;
  private final ApplicationMapper applicationMapper;
  private final DeclarationMapper declarationMapper;
  private final EvidenceMapper evidenceMapper;
  private final ClientDetailsMapper clientDetailsMapper;
  private final UserContext userContext;
  private final ObjectMapper objectMapper;
  private final EventService eventService;

  /**
   * Create an application.
   *
   * @return the full {@link ApplicationResponse} of the newly created application.
   * @throws ProviderOfficeNotAuthorizedException if the requested provider office code is not one
   *     of the user's authorized office codes
   * @throws DuplicateUfnException if the UFN is already used by another application with the same
   *     provider office code
   */
  @Transactional
  public ApplicationResponse createApplication(StartApplicationCommand startApplication) {
    validateProviderOfficeCode(startApplication.getProviderOfficeCode());

    String ufn = startApplication.getUfn();
    if (ufn != null
        && !ufn.isBlank()
        && repository.existsByProviderOfficeCodeAndUfn(
            startApplication.getProviderOfficeCode(), ufn)) {
      throw new DuplicateUfnException(ufn, startApplication.getProviderOfficeCode());
    }

    ApplicationEntity entity = applicationMapper.toApplicationEntity(startApplication);

    entity.setApplicationState(ApplicationState.DRAFT);
    ApplicationEntity saved = repository.save(entity);
    entityManager.refresh(saved);
    eventService.record(startApplication, startApplication.getProviderOfficeCode());
    return applicationMapper.toApplication(saved);
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
        ApplicationSpecification.filterByProviderContractInformation(
            userContext.getProviderFirmCode(), userContext.getOfficeCodes());
    if (additionalFilteringSpecification != null) {
      specificationToApply = specificationToApply.and(additionalFilteringSpecification);
    }
    final Sort defaultSort = Sort.by(Sort.Direction.DESC, "modifiedAt");
    return repository
        .findAll(specificationToApply, PageRequest.of(resolvedPage, resolvedSize, defaultSort))
        .map(applicationMapper::toApplicationSummary);
  }

  /**
   * Gets an Application or empty optional if not found.
   *
   * @return {@link ApplicationResponse}
   * @throws ProviderOfficeNotAuthorizedException if the application's provider office code is not
   *     one of the user's authorized office codes
   */
  public Optional<ApplicationResponse> getApplication(UUID applicationId) {
    var findApplicationByIdSpecification =
        ApplicationSpecification.findById(applicationId, userContext.getProviderFirmCode());
    Optional<ApplicationEntity> applicationOpt =
        repository.findOne(findApplicationByIdSpecification);
    applicationOpt.ifPresent(
        application -> validateProviderOfficeCode(application.getProviderOfficeCode()));
    return applicationOpt.map(applicationMapper::toApplication);
  }

  /**
   * Update means data for an application.
   *
   * @param applicationId the application ID
   * @param command the means data command including eTag for optimistic concurrency control
   * @return an OptionalLong containing the new ETag if updated, empty if not found
   * @throws EtagMismatchException if the eTag does not match the current entity value
   * @throws ProviderOfficeNotAuthorizedException if the application's provider office code is not
   *     one of the user's authorized office codes
   */
  @Transactional
  public OptionalLong updateMeansData(UUID applicationId, UpdateMeansDataCommand command) {
    Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmCode()));
    if (applicationOpt.isEmpty()) {
      return OptionalLong.empty();
    }

    ApplicationEntity application = applicationOpt.get();
    validateProviderOfficeCode(application.getProviderOfficeCode());
    validateEtag(application, command.geteTag());

    com.fasterxml.jackson.databind.JsonNode resultJson =
        objectMapper.valueToTree(command.getResult());
    Boolean indication = extractEligibilityIndication(resultJson);

    EligibilityResultEntity resultEntity =
        EligibilityResultEntity.builder()
            .applicationId(applicationId)
            .data(objectMapper.valueToTree(command.getData()))
            .resultJson(resultJson)
            .indication(indication)
            .createdBy(userContext.getCurrentUser())
            .build();

    eligibilityResultRepository.save(resultEntity);

    application.setModifiedBy(userContext.getCurrentUser());
    ApplicationEntity saved = repository.save(application);
    eventService.record(command, application.getProviderOfficeCode());
    return OptionalLong.of(saved.getEtag());
  }

  private Boolean extractEligibilityIndication(com.fasterxml.jackson.databind.JsonNode resultJson) {
    if (resultJson == null) {
      return null;
    }
    com.fasterxml.jackson.databind.JsonNode overallResult =
        resultJson.at("/result_summary/overall_result/result");
    if (overallResult == null || overallResult.isMissingNode()) {
      return null;
    }
    String result = overallResult.asText();
    if ("eligible".equalsIgnoreCase(result)) {
      return true;
    } else if ("ineligible".equalsIgnoreCase(result)) {
      return false;
    }
    return null;
  }

  /**
   * Update application client declaration.
   *
   * @param command the declaration command including eTag for optimistic concurrency control
   * @return an OptionalLong containing the new ETag if updated, empty if not found
   * @throws EtagMismatchException if the eTag does not match the current entity value
   * @throws ProviderOfficeNotAuthorizedException if the application's provider office code is not
   *     one of the user's authorized office codes
   */
  @Transactional
  public OptionalLong updateClientDeclaration(UUID applicationId, DeclarationCommand command) {
    final Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmCode()));
    if (applicationOpt.isEmpty()) {
      return OptionalLong.empty();
    }

    final ApplicationEntity application = applicationOpt.get();
    validateProviderOfficeCode(application.getProviderOfficeCode());
    validateEtag(application, command.geteTag());

    DeclarationEntity declarationEntity = declarationMapper.toDeclarationEntity(command);
    // TODO: declaration status is currently undefined
    declarationEntity.setClientDeclarationStatus(ClientDeclarationStatus.DRAFT);

    if (application.getDeclaration() != null) {
      declarationEntity.setId(application.getDeclaration().getId());
      declarationEntity.setCreatedAt(application.getDeclaration().getCreatedAt());
      declarationEntity.setCreatedBy(application.getDeclaration().getCreatedBy());
    }
    application.setDeclaration(declarationEntity);

    ApplicationEntity saved = repository.save(application);
    eventService.record(command, application.getProviderOfficeCode());
    return OptionalLong.of(saved.getEtag());
  }

  /**
   * Update application evidence.
   *
   * @param command the evidence command including eTag for optimistic concurrency control
   * @return an OptionalLong containing the new ETag if updated, empty if not found
   * @throws EtagMismatchException if the eTag does not match the current entity value
   * @throws ProviderOfficeNotAuthorizedException if the application's provider office code is not
   *     one of the user's authorized office codes
   */
  @Transactional
  public OptionalLong updateEvidence(UUID applicationId, UpdateEvidenceCommand command) {
    final Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmCode()));
    if (applicationOpt.isEmpty()) {
      return OptionalLong.empty();
    }
    final ApplicationEntity application = applicationOpt.get();
    validateProviderOfficeCode(application.getProviderOfficeCode());
    validateEtag(application, command.geteTag());

    EvidenceEntity evidenceEntity = evidenceMapper.toEvidenceEntity(command);
    if (application.getEvidence() != null) {
      evidenceEntity.setEvidenceId(application.getEvidence().getEvidenceId());
      evidenceEntity.setCreatedAt(application.getEvidence().getCreatedAt());
      evidenceEntity.setCreatedBy(application.getEvidence().getCreatedBy());
    } else {
      evidenceEntity.setCreatedBy(userContext.getCurrentUser());
    }
    evidenceEntity.setModifiedBy(userContext.getCurrentUser());
    EvidenceEntity savedEvidence = evidenceRepository.save(evidenceEntity);

    application.setEvidence(savedEvidence);
    application.setModifiedBy(userContext.getCurrentUser());
    ApplicationEntity saved = repository.save(application);
    eventService.record(command, application.getProviderOfficeCode());
    return OptionalLong.of(saved.getEtag());
  }

  private void validateEtag(ApplicationEntity application, Long providedEtag) {
    if (application.getEtag() != providedEtag) {
      throw new EtagMismatchException(providedEtag, application.getEtag());
    }
  }

  private void validateProviderOfficeCode(String providerOfficeCode) {
    if (!userContext.getOfficeCodes().contains(providerOfficeCode)) {
      throw new ProviderOfficeNotAuthorizedException(providerOfficeCode);
    }
  }

  /**
   * Update scoping data for an application.
   *
   * @param applicationId the application ID
   * @param command the scoping data command including eTag for optimistic concurrency control
   * @return an OptionalLong containing the new ETag if updated, empty if not found
   * @throws EtagMismatchException if the eTag does not match the current entity value
   * @throws ProviderOfficeNotAuthorizedException if the application's provider office code is not
   *     one of the user's authorized office codes
   */
  @Transactional
  public OptionalLong updateScopingData(UUID applicationId, UpdateScopingDataCommand command) {
    Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmCode()));
    if (applicationOpt.isEmpty()) {
      return OptionalLong.empty();
    }

    ApplicationEntity application = applicationOpt.get();
    validateProviderOfficeCode(application.getProviderOfficeCode());
    validateEtag(application, command.geteTag());

    application.setScopingQuestions(objectMapper.valueToTree(command.getScopingQuestions()));
    application.setModifiedBy(userContext.getCurrentUser());
    ApplicationEntity saved = repository.save(application);
    eventService.record(command, application.getProviderOfficeCode());
    return OptionalLong.of(saved.getEtag());
  }

  /**
   * Update an application.
   *
   * @param applicationId the application ID
   * @param command the update command including eTag for optimistic concurrency control
   * @return an OptionalLong containing the new ETag if updated, empty if not found
   * @throws EtagMismatchException if the eTag does not match the current entity value
   * @throws ProviderOfficeNotAuthorizedException if the application's provider office code is not
   *     one of the user's authorized office codes
   */
  @Transactional
  public OptionalLong updateApplication(UUID applicationId, UpdateApplicationCommand command) {
    Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmCode()));
    if (applicationOpt.isEmpty()) {
      return OptionalLong.empty();
    }

    ApplicationEntity application = applicationOpt.get();
    validateProviderOfficeCode(application.getProviderOfficeCode());
    validateEtag(application, command.geteTag());

    applicationMapper.updateApplicationEntity(command, application);
    ApplicationEntity saved = repository.save(application);
    eventService.record(command, application.getProviderOfficeCode());
    return OptionalLong.of(saved.getEtag());
  }

  /**
   * Edit application metadata fields, and optionally the linked client details (and address),
   * declaration, and evidence in the same request. Only fields present on the command (and its
   * nested objects) are changed; omitted fields are left unchanged. Declaration and evidence are
   * created if they don't already exist on the application. A declaration that has already been
   * signed (i.e. has a {@code dateSigned}) is immutable and cannot be patched.
   *
   * @param applicationId the application ID
   * @param command the edit command including eTag for optimistic concurrency control
   * @return an OptionalLong containing the new ETag if updated, empty if not found
   * @throws EtagMismatchException if the eTag does not match the current entity value
   * @throws ProviderOfficeNotAuthorizedException if the application's provider office code is not
   *     one of the user's authorized office codes
   * @throws DuplicateUfnException if the UFN is already used by another application with the same
   *     provider office code
   * @throws DeclarationAlreadySignedException if a declaration patch is supplied but the existing
   *     declaration has already been signed
   */
  @Transactional
  public OptionalLong editApplication(UUID applicationId, EditApplicationCommand command) {
    Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmCode()));
    if (applicationOpt.isEmpty()) {
      return OptionalLong.empty();
    }

    ApplicationEntity application = applicationOpt.get();
    validateProviderOfficeCode(application.getProviderOfficeCode());
    validateEtag(application, command.geteTag());

    String ufn = command.getUfn();
    if (ufn != null
        && !ufn.isBlank()
        && repository.existsByProviderOfficeCodeAndUfnAndIdNot(
            application.getProviderOfficeCode(), ufn, applicationId)) {
      throw new DuplicateUfnException(ufn, application.getProviderOfficeCode());
    }

    validateClientPatch(application.getClientDetails(), command.getClientDetails());
    applicationMapper.editApplicationEntity(command, application);
    applyApplicationPatch(command, application);

    if (command.getClientDetails() != null) {
      applyClientPatch(command.getClientDetails(), application.getClientDetails());
    }

    if (command.getDeclaration() != null) {
      DeclarationEntity declaration = application.getDeclaration();
      if (declaration == null) {
        declaration = new DeclarationEntity();
        declaration.setClientDeclarationStatus(ClientDeclarationStatus.DRAFT);
        declaration.setCreatedBy(userContext.getCurrentUser());
      } else if (declaration.getDateSigned() != null) {
        throw new DeclarationAlreadySignedException(applicationId);
      }
      declarationMapper.patchDeclarationEntity(command.getDeclaration(), declaration);
      application.setDeclaration(declaration);
    }

    if (command.getEvidence() != null) {
      EvidenceEntity evidence = application.getEvidence();
      if (evidence == null) {
        evidence = new EvidenceEntity();
        evidence.setCreatedBy(userContext.getCurrentUser());
      }
      evidenceMapper.patchEvidenceEntity(command.getEvidence(), evidence);
      evidence.setModifiedBy(userContext.getCurrentUser());
      application.setEvidence(evidenceRepository.save(evidence));
    }

    ApplicationEntity saved = repository.save(application);
    repository.flush();
    eventService.record(command, application.getProviderOfficeCode());
    return OptionalLong.of(saved.getEtag());
  }

  private void applyApplicationPatch(
      EditApplicationCommand command, ApplicationEntity application) {
    JsonNullable<String> reasonForReapplication = command.getReasonForReapplication();
    if (isPresent(reasonForReapplication) && reasonForReapplication.get() != null) {
      application.setReasonForReapplication(reasonForReapplication.get());
    }
    applyNullable(command.getEcfFlag(), application::setEcfFlag);
    applyScopingQuestions(command.getScopingQuestions(), application);
  }

  private void applyClientPatch(PatchClientDetailsData command, ClientDetailsEntity clientDetails) {
    clientDetailsMapper.patchClientDetailsEntity(command, clientDetails);
    applyNullable(command.getNiNumber(), clientDetails::setNiNumber);
    applyAddressPatch(command.getAddress(), clientDetails);
  }

  private void validateClientPatch(
      ClientDetailsEntity clientDetails, PatchClientDetailsData command) {
    if (clientDetails == null
        || command == null
        || (command.getNoFixedAbode() == null && !isPresent(command.getAddress()))) {
      return;
    }

    boolean noFixedAbode =
        command != null && command.getNoFixedAbode() != null
            ? command.getNoFixedAbode()
            : clientDetails.isNoFixedAbode();
    AddressEntity address = proposedAddress(clientDetails.getAddress(), command);

    if (noFixedAbode && address != null) {
      throw new InvalidClientDetailsPatchException(
          "A client with no fixed abode cannot have an address");
    }
    if (!noFixedAbode && address == null) {
      throw new InvalidClientDetailsPatchException(
          "A client with a fixed address must have an address");
    }
  }

  private AddressEntity proposedAddress(
      AddressEntity existingAddress, PatchClientDetailsData command) {
    if (command == null || !isPresent(command.getAddress())) {
      return existingAddress;
    }
    PatchAddressData addressCommand = command.getAddress().get();
    if (addressCommand == null) {
      return null;
    }

    if (existingAddress == null
        && (addressCommand.getAddressLine1() == null || addressCommand.getCountry() == null)) {
      throw new InvalidClientDetailsPatchException(
          "An address must have an addressLine1 and country when created");
    }
    return existingAddress == null ? new AddressEntity() : existingAddress;
  }

  private void applyAddressPatch(
      JsonNullable<PatchAddressData> command, ClientDetailsEntity clientDetails) {
    if (!isPresent(command)) {
      return;
    }
    PatchAddressData addressCommand = command.get();
    if (addressCommand == null) {
      clientDetails.setAddress(null);
      return;
    }

    AddressEntity address = clientDetails.getAddress();
    if (address == null) {
      address = new AddressEntity();
      address.setCreatedBy(userContext.getCurrentUser());
      clientDetails.setAddress(address);
    }
    address.setModifiedBy(userContext.getCurrentUser());
    if (addressCommand.getAddressLine1() != null) {
      address.setAddressLine1(addressCommand.getAddressLine1());
    }
    if (addressCommand.getCountry() != null) {
      address.setCountry(addressCommand.getCountry());
    }
    applyAddressFields(addressCommand, address);
  }

  private void applyAddressFields(PatchAddressData command, AddressEntity address) {
    applyNullable(command.getAddressLine2(), address::setAddressLine2);
    applyNullable(command.getAddressLine3(), address::setAddressLine3);
    applyNullable(command.getAddressLine4(), address::setAddressLine4);
    applyNullable(command.getTownOrCity(), address::setTownOrCity);
    applyNullable(command.getPostCode(), address::setPostCode);
    applyNullable(command.getCounty(), address::setCounty);
  }

  private void applyScopingQuestions(
      JsonNullable<java.util.Map<String, Object>> command, ApplicationEntity application) {
    if (!isPresent(command)) {
      return;
    }
    if (command.get() == null) {
      application.setScopingQuestions(null);
      return;
    }

    ObjectNode merged =
        application.getScopingQuestions() != null && application.getScopingQuestions().isObject()
            ? application.getScopingQuestions().deepCopy()
            : objectMapper.createObjectNode();
    command
        .get()
        .forEach(
            (key, value) -> {
              if (value == null) {
                merged.remove(key);
              } else {
                merged.set(key, objectMapper.valueToTree(value));
              }
            });
    application.setScopingQuestions(merged);
  }

  private <T> void applyNullable(JsonNullable<T> value, java.util.function.Consumer<T> setter) {
    if (isPresent(value)) {
      setter.accept(value.get());
    }
  }

  private boolean isPresent(JsonNullable<?> value) {
    return value != null && value.isPresent();
  }

  /**
   * Update client details (and address) for an application.
   *
   * @param applicationId the application ID
   * @param command the update command including eTag for optimistic concurrency control
   * @return an OptionalLong containing the new ETag if updated, empty if not found
   * @throws EtagMismatchException if the eTag does not match the current entity value
   * @throws ProviderOfficeNotAuthorizedException if the application's provider office code is not
   *     one of the user's authorized office codes
   */
  @Transactional
  public OptionalLong updateClientDetails(UUID applicationId, UpdateClientDetailsCommand command) {
    Optional<ApplicationEntity> applicationOpt =
        repository.findOne(
            ApplicationSpecification.findById(applicationId, userContext.getProviderFirmCode()));
    if (applicationOpt.isEmpty()) {
      return OptionalLong.empty();
    }

    ApplicationEntity application = applicationOpt.get();
    validateProviderOfficeCode(application.getProviderOfficeCode());
    validateEtag(application, command.geteTag());

    ClientDetailsEntity clientDetails = application.getClientDetails();
    clientDetailsMapper.updateClientDetailsEntity(command, clientDetails);

    application.setModifiedBy(userContext.getCurrentUser());
    ApplicationEntity saved = repository.save(application);
    eventService.record(command, application.getProviderOfficeCode());
    return OptionalLong.of(saved.getEtag());
  }
}
