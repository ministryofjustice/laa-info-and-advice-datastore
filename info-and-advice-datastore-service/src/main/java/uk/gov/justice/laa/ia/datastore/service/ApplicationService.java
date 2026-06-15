package uk.gov.justice.laa.ia.datastore.service;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;

/** Service class for handling Applications. */
@RequiredArgsConstructor
@Service
public class ApplicationService {

  private static final int DEFAULT_PAGE_SIZE = 25;

  private final ApplicationRepository repository;
  private final ApplicationMapper applicationMapper;
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
}
