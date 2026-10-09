package uk.gov.justice.laa.ia.datastore.scheduled;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;
import uk.gov.justice.laa.ia.datastore.models.ClaimsModel;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.service.RetentionDateUpdateService;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;

/**
 * Scheduled task that sets the retention date for applications to be deleted for based on data
 * retention policies.
 */
@Component
@Slf4j
public class DataRetentionDateResolverTask {
  private final ClaimsGateway claimsGateway;
  private final int dataRetentionYearsOffset;
  private final ApplicationRepository applicationRepository;
  private final RetentionDateUpdateService retentionDateUpdateService;
  private static final String STATUS_TO_CHECK = "APPROVED";

  /** Constructs the resolver, binding the configured data retention years offset. */
  public DataRetentionDateResolverTask(
      ClaimsGateway claimsGateway,
      @Value("${laa.datastore.data-retention.years-offset}") int dataRetentionYearsOffset,
      ApplicationRepository applicationRepository,
      RetentionDateUpdateService retentionDateUpdateService) {
    this.claimsGateway = claimsGateway;
    this.dataRetentionYearsOffset = dataRetentionYearsOffset;
    this.applicationRepository = applicationRepository;
    this.retentionDateUpdateService = retentionDateUpdateService;
  }

  /**
   * Executes the data retention date resolution process for all applications missing a retention
   * date. Invoked by a profile-specific scheduler bean rather than scheduled directly. Each
   * application is saved and recorded in its own transaction so one failure doesn't prevent others
   * in the collection from being processed.
   */
  public void run() {
    final var applications =
        applicationRepository.findAll(
            ApplicationSpecification.findByMissingDataRetentionDateAndStatus());
    log.info("Found {} applications missing a retention date", applications.size());
    for (var application : applications) {
      RetentionDateUpdate update = shouldUpdateDataRetentionDate(application);
      if (update.shouldUpdate) {
        Instant previousRetentionDate = application.getDataRetentionDate();
        Instant newRetentionDate =
            update
                .newRetentionDate
                .plus(dataRetentionYearsOffset, java.time.temporal.ChronoUnit.YEARS)
                .toInstant();
        try {
          retentionDateUpdateService.saveAndRecord(
              application.getId(),
              application.getEtag(),
              previousRetentionDate,
              newRetentionDate,
              update.claimsId());
          log.debug("Updated data retention date for application with ID {}", application.getId());
        } catch (Exception e) {
          log.error(
              "Error while saving updated data retention date "
                  + "for application with ID {}, skipping application",
              application.getId(),
              e);
        }
      }
    }
  }

  private record RetentionDateUpdate(
      boolean shouldUpdate, OffsetDateTime newRetentionDate, UUID claimsId) {
    static RetentionDateUpdate hasApproval(OffsetDateTime newRetentionDate, UUID claimsId) {
      return new RetentionDateUpdate(true, newRetentionDate, claimsId);
    }

    static RetentionDateUpdate noApproval() {
      return new RetentionDateUpdate(false, null, null);
    }
  }

  private RetentionDateUpdate shouldUpdateDataRetentionDate(ApplicationEntity application) {
    try {
      final var response =
          claimsGateway.getClaims(application.getProviderOfficeCode(), application.getUfn());
      final var latestClaim = getLatestClaim(response);
      if (latestClaim != null) {
        return RetentionDateUpdate.hasApproval(
            latestClaim.getCreatedOn(), latestClaim.getClaimId());
      }
      return RetentionDateUpdate.noApproval();
    } catch (Exception e) {
      log.error(
          "Error while resolving data retention date "
              + "for application with ID {}, skipping application",
          application.getId(),
          e);
      return RetentionDateUpdate.noApproval();
    }
  }

  private static ClaimsModel getLatestClaim(ApplicationClaimResponse response) {
    return hasClaims(response)
        ? response.getClaims().stream()
            .filter(claim -> STATUS_TO_CHECK.equals(claim.getStatus()))
            .sorted((c1, c2) -> c2.getCreatedOn().compareTo(c1.getCreatedOn()))
            .findFirst()
            .orElse(null)
        : null;
  }

  private static boolean hasClaims(ApplicationClaimResponse response) {
    return response != null && response.getClaims() != null && !response.getClaims().isEmpty();
  }
}
