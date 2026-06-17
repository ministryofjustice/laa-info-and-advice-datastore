package uk.gov.justice.laa.ia.datastore.service;

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
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.mapper.DeclarationMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;

/** Service class for handling Applications. */
@RequiredArgsConstructor
@Service
public class ApplicationService {

  private static final int DEFAULT_PAGE_SIZE = 25;

  private final ApplicationRepository repository;
  private final ApplicationMapper applicationMapper;
  private final DeclarationMapper declarationMapper;
  private final UserContext userContext;

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
