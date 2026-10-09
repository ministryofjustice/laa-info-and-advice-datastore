package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;
import uk.gov.justice.laa.ia.datastore.scheduled.DataDeletionTask;

/**
 * Deletes PII associated with an application whose data retention period has expired and marks it
 * as soft-deleted, each in its own transaction so a failure for one application does not roll back
 * others.
 */
@Component
@Slf4j
public class ApplicationDataDeletionService {
  private final ApplicationRepository applicationRepository;
  private final EventRepository eventRepository;
  private final SystemDrivenEventService systemDrivenEventService;

  /**
   * Constructor for ApplicationDataDeletionService.
   *
   * @param applicationRepository the repository for accessing application entities
   * @param eventRepository the repository for accessing event entities
   * @param systemDrivenEventService the service for recording system-driven events
   */
  public ApplicationDataDeletionService(
      ApplicationRepository applicationRepository,
      EventRepository eventRepository,
      SystemDrivenEventService systemDrivenEventService) {
    this.applicationRepository = applicationRepository;
    this.eventRepository = eventRepository;
    this.systemDrivenEventService = systemDrivenEventService;
  }

  /**
   * Re-fetches the application, nulls out its PII data (client details, address, and any related
   * event history), and marks it as soft-deleted. Committed independently of the caller's
   * transaction (if any). If the application's eTag has changed since it was read by the caller,
   * the deletion is skipped rather than overwriting a concurrent change.
   */
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void deleteAndRecord(UUID applicationId, long expectedEtag) {
    ApplicationEntity application =
        applicationRepository
            .findById(applicationId)
            .orElseThrow(
                () -> new EntityNotFoundException("Application not found: " + applicationId));
    if (application.getEtag() != expectedEtag) {
      log.warn(
          "Skipping data deletion for application with ID {}, eTag has changed since it was read "
              + "(expected {}, actual {})",
          applicationId,
          expectedEtag,
          application.getEtag());
      return;
    }

    deleteClientDetailsPii(application.getClientDetails());
    Instant deletedAt = Instant.now();
    application.setDeleted(true);
    application.setDeletedAt(deletedAt);
    applicationRepository.save(application);

    for (var event : eventRepository.findByApplicationId(applicationId)) {
      event.setPiiData(null);
      eventRepository.save(event);
    }

    ObjectNode payload =
        JsonNodeFactory.instance
            .objectNode()
            .put("deleted", true)
            .put("deletedAt", deletedAt.toString());
    systemDrivenEventService.record(
        payload,
        application.getProviderOfficeCode(),
        application.getProviderFirmCode(),
        DataDeletionTask.class.getSimpleName(),
        applicationId);
  }

  private void deleteClientDetailsPii(ClientDetailsEntity clientDetails) {
    clientDetails.setFirstName(null);
    clientDetails.setLastName(null);
    clientDetails.setDateOfBirth(null);
    clientDetails.setNiNumber(null);
    AddressEntity address = clientDetails.getAddress();
    if (address != null) {
      address.setAddressLine1(null);
      address.setAddressLine2(null);
      address.setAddressLine3(null);
      address.setAddressLine4(null);
      address.setTownOrCity(null);
      address.setPostCode(null);
      address.setCounty(null);
    }
  }
}
