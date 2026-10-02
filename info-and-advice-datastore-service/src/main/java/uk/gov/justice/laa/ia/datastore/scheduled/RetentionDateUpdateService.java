package uk.gov.justice.laa.ia.datastore.scheduled;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;

/**
 * Persists a single application's data retention date update and records the change as an event,
 * each in its own transaction so a failure for one application does not roll back others.
 */
@Component
@Slf4j
public class RetentionDateUpdateService {
  private final ApplicationRepository applicationRepository;
  private final SystemDrivenEventService systemDrivenEventService;

  public RetentionDateUpdateService(
      ApplicationRepository applicationRepository,
      SystemDrivenEventService systemDrivenEventService) {
    this.applicationRepository = applicationRepository;
    this.systemDrivenEventService = systemDrivenEventService;
  }

  /**
   * Re-fetches the application, then saves the new retention date and records the change. Committed
   * independently of the caller's transaction (if any). If the application's eTag has changed since
   * it was read by the caller, the update is skipped rather than overwriting a concurrent change.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void saveAndRecord(
      UUID applicationId,
      long expectedEtag,
      Instant previousRetentionDate,
      Instant newRetentionDate,
      UUID claimsId) {
    ApplicationEntity application =
        applicationRepository
            .findById(applicationId)
            .orElseThrow(
                () -> new EntityNotFoundException("Application not found: " + applicationId));
    if (application.getEtag() != expectedEtag) {
      log.warn(
          "Skipping data retention date update for application with ID {}, eTag has changed "
              + "since it was read (expected {}, actual {})",
          applicationId,
          expectedEtag,
          application.getEtag());
      return;
    }

    ObjectNode dataRetentionDate =
        JsonNodeFactory.instance
            .objectNode()
            .put("dataRetentionDate", newRetentionDate.toString())
            .put("claimsId", claimsId.toString());
    application.setDataRetentionDate(newRetentionDate);
    applicationRepository.save(application);
    systemDrivenEventService.record(
        dataRetentionDate,
        application.getProviderOfficeCode(),
        application.getProviderFirmCode(),
        DataRetentionDateResolver.class.getSimpleName(),
        applicationId);
  }
}
