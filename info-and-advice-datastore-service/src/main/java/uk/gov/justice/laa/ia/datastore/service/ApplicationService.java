package uk.gov.justice.laa.ia.datastore.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.mapper.ApplicationMapper;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;

/** Service class for handling Applications. */
@RequiredArgsConstructor
@Service
public class ApplicationService {
  private final ApplicationRepository repository;
  private final ApplicationMapper applicationMapper;

  /**
   * Create an application.
   *
   * @return ID of newly created application.
   */
  public UUID createApplication(StartCaseCommand startCase) {
    return null;
  }

  /**
   * Gets all the Applications.
   *
   * @return list of {@link ApplicationResponse}
   */
  public List<ApplicationResponse> getAllApplications() {
    return repository.findAll().stream().map(applicationMapper::toApplication).toList();
  }

  /**
   * Gets an Application or empty optional if not found.
   *
   * @return {@link ApplicationResponse}
   */
  public Optional<ApplicationResponse> getApplication(UUID applicationId) {
    return repository.findById(applicationId).map(applicationMapper::toApplication);
  }
}
