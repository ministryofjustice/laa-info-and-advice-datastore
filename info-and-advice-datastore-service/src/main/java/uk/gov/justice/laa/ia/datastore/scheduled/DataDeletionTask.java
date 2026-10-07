package uk.gov.justice.laa.ia.datastore.scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;
import uk.gov.justice.laa.ia.datastore.models.ClaimsModel;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;
import uk.gov.justice.laa.ia.datastore.service.RetentionDateUpdateService;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;

/**
 * Task responsible for deleting completed applications that have passed their data retention period
 * with no additional claims.
 */
@Component
@Slf4j
public class DataDeletionTask {
  private static final String APPROVED_STATUS = "APPROVED";

  private final ApplicationRepository applicationRepository;
  private final ClaimsGateway claimsGateway;
  private final int dataRetentionYearsOffset;
  private final RetentionDateUpdateService retentionDateUpdateService;
  private final ApplicationDataDeletionService applicationDataDeletionService;

  /** Constructs the task, binding the configured data retention years offset. */
  public DataDeletionTask(
      ApplicationRepository applicationRepository,
      ClaimsGateway claimsGateway,
      @Value("${laa.datastore.data-retention.years-offset}") int dataRetentionYearsOffset,
      RetentionDateUpdateService retentionDateUpdateService,
      ApplicationDataDeletionService applicationDataDeletionService) {
    this.applicationRepository = applicationRepository;
    this.claimsGateway = claimsGateway;
    this.dataRetentionYearsOffset = dataRetentionYearsOffset;
    this.retentionDateUpdateService = retentionDateUpdateService;
    this.applicationDataDeletionService = applicationDataDeletionService;
  }

  /**
   * Executes the data deletion process for all completed applications whose data retention date has
   * expired. Each application is either extended (if claims indicate the retention period has not
   * really passed) or deleted, each in its own transaction so one failure doesn't prevent others in
   * the collection from being processed.
   */
  public void run() {
    final var applications =
        applicationRepository.findAll(
            ApplicationSpecification.findByExpiredDataRetentionDateAndStatus());
    log.info("Found {} applications with an expired data retention date", applications.size());
    for (var application : applications) {
      try {
        processApplication(application);
      } catch (Exception e) {
        log.error(
            "Error while processing data deletion for application with ID {}, skipping application",
            application.getId(),
            e);
      }
    }
  }

  private void processApplication(ApplicationEntity application) {
    final ClaimsModel latestClaim;
    try {
      final var response =
          claimsGateway.getClaims(application.getProviderOfficeCode(), application.getUfn());
      latestClaim = getLatestClaim(response);
    } catch (Exception e) {
      log.error(
          "Error while fetching claims for application with ID {}, skipping application",
          application.getId(),
          e);
      return;
    }

    if (latestClaim == null) {
      log.error(
          "Latest claim should not be null for a completed application with a data retention date"
              + " as it should only have been set when a claim was originally found"
              + ", skipping deleting or updating the retention date for application {}",
          application.getId());
      return;
    }

    if (APPROVED_STATUS.equals(latestClaim.getStatus())) {
      Instant candidateRetentionDate =
          latestClaim.getCreatedOn().plus(dataRetentionYearsOffset, ChronoUnit.YEARS).toInstant();
      if (candidateRetentionDate.isAfter(Instant.now())) {
        retentionDateUpdateService.saveAndRecord(
            application.getId(),
            application.getEtag(),
            application.getDataRetentionDate(),
            candidateRetentionDate,
            latestClaim.getClaimId());
        return;
      }
    }

    applicationDataDeletionService.deleteAndRecord(application.getId(), application.getEtag());
  }

  private static ClaimsModel getLatestClaim(ApplicationClaimResponse response) {
    return hasClaims(response)
        ? response.getClaims().stream()
            .sorted((c1, c2) -> c2.getCreatedOn().compareTo(c1.getCreatedOn()))
            .findFirst()
            .orElse(null)
        : null;
  }

  private static boolean hasClaims(ApplicationClaimResponse response) {
    return response != null && response.getClaims() != null && !response.getClaims().isEmpty();
  }
}
