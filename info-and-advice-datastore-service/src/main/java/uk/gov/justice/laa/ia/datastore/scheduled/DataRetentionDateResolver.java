package uk.gov.justice.laa.ia.datastore.scheduled;

import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.models.ApplicationClaimResponse;
import uk.gov.justice.laa.ia.datastore.models.ClaimsModel;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;

/**
 * Scheduled task that sets the retention date for applications to be deleted for based on data
 * retention policies.
 */
@Component
@Slf4j
public class DataRetentionDateResolver {
  private final ClaimsGateway claimsGateway;
  private final int dataRetentionYearsOffset;
  private final ApplicationRepository applicationRepository;
  private static final String STATUS_TO_CHECK = "APPROVED";

  /** Constructs the resolver, binding the configured data retention years offset. */
  public DataRetentionDateResolver(
      ClaimsGateway claimsGateway,
      @Value("${laa.datastore.data-retention.years-offset}") int dataRetentionYearsOffset,
      ApplicationRepository applicationRepository) {
    this.claimsGateway = claimsGateway;
    this.dataRetentionYearsOffset = dataRetentionYearsOffset;
    this.applicationRepository = applicationRepository;
  }

  /**
   * Executes the data retention date resolution process for all applications missing a retention
   * date. Invoked by a profile-specific scheduler bean rather than scheduled directly.
   */
  public void run() {
    final var applications =
        applicationRepository.findAll(
            ApplicationSpecification.findByMissingDataRetentionDateAndStatus());
    log.info("Found {} applications missing a retention date", applications.size());
    for (var application : applications) {
      RetentionDateUpdate update = shouldUpdateDataRetentionDate(application);
      if (update.shouldUpdate) {
        // Logic to update the data retention date for the application goes here
        application.setDataRetentionDate(
            update
                .newRetentionDate
                .plus(dataRetentionYearsOffset, java.time.temporal.ChronoUnit.YEARS)
                .toInstant());
        applicationRepository.save(application);
        log.debug("Updated data retention date for application with ID {}", application.getId());
      }
    }
  }

  private record RetentionDateUpdate(boolean shouldUpdate, OffsetDateTime newRetentionDate) {
    static RetentionDateUpdate hasApproval(OffsetDateTime newRetentionDate) {
      return new RetentionDateUpdate(true, newRetentionDate);
    }

    static RetentionDateUpdate noApproval() {
      return new RetentionDateUpdate(false, null);
    }
  }

  private RetentionDateUpdate shouldUpdateDataRetentionDate(ApplicationEntity application) {
    try {
      final var response =
          claimsGateway.getClaims(application.getProviderOfficeCode(), application.getUfn());
      final var latestClaim = getLatestClaim(response);
      if (latestClaim != null && STATUS_TO_CHECK.equals(latestClaim.getStatus())) {
        return RetentionDateUpdate.hasApproval(latestClaim.getCreatedOn());
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
            .sorted((c1, c2) -> c2.getCreatedOn().compareTo(c1.getCreatedOn()))
            .findFirst()
            .orElse(null)
        : null;
  }

  private static boolean hasClaims(ApplicationClaimResponse response) {
    return response != null && response.getClaims() != null && !response.getClaims().isEmpty();
  }
}
