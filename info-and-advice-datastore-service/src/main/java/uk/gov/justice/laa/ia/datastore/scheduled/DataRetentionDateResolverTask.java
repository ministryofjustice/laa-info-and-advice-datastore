package uk.gov.justice.laa.ia.datastore.scheduled;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
  private final int ineligibleRetentionYearsOffset;
  private final ApplicationRepository applicationRepository;
  private final RetentionDateUpdateService retentionDateUpdateService;

  /** Constructs the resolver, binding the configured data retention years offsets. */
  public DataRetentionDateResolverTask(
      ClaimsGateway claimsGateway,
      @Value("${laa.datastore.data-retention.years-offset}") int dataRetentionYearsOffset,
      @Value("${laa.datastore.data-retention.ineligible-years-offset}")
          int ineligibleRetentionYearsOffset,
      ApplicationRepository applicationRepository,
      RetentionDateUpdateService retentionDateUpdateService) {
    this.claimsGateway = claimsGateway;
    this.dataRetentionYearsOffset = dataRetentionYearsOffset;
    this.ineligibleRetentionYearsOffset = ineligibleRetentionYearsOffset;
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
    try (var ignored = MDC.putCloseable("correlationId", UUID.randomUUID().toString())) {
      runInternal();
    }
  }

  private void runInternal() {
    final var applications =
        applicationRepository.findAll(
            ApplicationSpecification.findByMissingDataRetentionDateAndStatus());
    log.info("Found {} applications missing a retention date", applications.size());
    for (var application : applications) {
      RetentionDateUpdate update = shouldUpdateDataRetentionDate(application);
      if (update.shouldUpdate) {
        Instant previousRetentionDate = application.getDataRetentionDate();
        try {
          retentionDateUpdateService.saveAndRecord(
              application.getId(),
              application.getEtag(),
              previousRetentionDate,
              update.newRetentionDate,
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
      boolean shouldUpdate, Instant newRetentionDate, UUID claimsId) {
    static RetentionDateUpdate hasApproval(Instant newRetentionDate, UUID claimsId) {
      return new RetentionDateUpdate(true, newRetentionDate, claimsId);
    }

    static RetentionDateUpdate noApproval() {
      return new RetentionDateUpdate(false, null, null);
    }
  }

  private RetentionDateUpdate shouldUpdateDataRetentionDate(ApplicationEntity application) {
    if (application.hasIneligibleMostRecentResult()) {
      // Ineligible applications never lead to a claim, so their date is based on the last
      // modification and the claims API isn't consulted.
      return RetentionDateUpdate.hasApproval(
          addYears(
              application.getModifiedAt().atOffset(ZoneOffset.UTC), ineligibleRetentionYearsOffset),
          null);
    }
    if (application.getUfn() == null || application.getUfn().isBlank()) {
      log.warn(
          "Completed application with ID {} has no UFN, skipping application", application.getId());
      return RetentionDateUpdate.noApproval();
    }
    try {
      final var response =
          claimsGateway.getClaims(application.getProviderOfficeCode(), application.getUfn());
      final var latestClaim = getLatestClaim(response);
      if (latestClaim != null) {
        return RetentionDateUpdate.hasApproval(
            addYears(latestClaim.getUpdatedOn(), dataRetentionYearsOffset),
            latestClaim.getClaimId());
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

  private static Instant addYears(OffsetDateTime from, int years) {
    return from.plus(years, ChronoUnit.YEARS).toInstant();
  }

  private static ClaimsModel getLatestClaim(ApplicationClaimResponse response) {
    return hasClaims(response)
        ? response.getClaims().stream()
            .sorted((c1, c2) -> c2.getUpdatedOn().compareTo(c1.getUpdatedOn()))
            .findFirst()
            .orElse(null)
        : null;
  }

  private static boolean hasClaims(ApplicationClaimResponse response) {
    return response != null && response.getClaims() != null && !response.getClaims().isEmpty();
  }
}
