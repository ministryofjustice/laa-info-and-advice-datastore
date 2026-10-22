package uk.gov.justice.laa.ia.datastore.scheduled;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;

/**
 * Task responsible for deleting draft applications that have not been modified within the
 * configured draft retention period. Unlike completed applications, drafts have no claims to
 * consider, so age alone decides deletion.
 */
@Component
@Slf4j
public class DraftDataDeletionTask {
  private final ApplicationRepository applicationRepository;
  private final int draftRetentionYearsOffset;
  private final ApplicationDataDeletionService applicationDataDeletionService;

  /** Constructs the task, binding the configured draft retention years offset. */
  public DraftDataDeletionTask(
      ApplicationRepository applicationRepository,
      @Value("${laa.datastore.data-retention.draft-years-offset}") int draftRetentionYearsOffset,
      ApplicationDataDeletionService applicationDataDeletionService) {
    this.applicationRepository = applicationRepository;
    this.draftRetentionYearsOffset = draftRetentionYearsOffset;
    this.applicationDataDeletionService = applicationDataDeletionService;
  }

  /**
   * Executes the deletion of all draft applications last modified before the retention cutoff. Each
   * application is deleted in its own transaction so one failure doesn't prevent others in the
   * collection from being processed.
   */
  public void run() {
    try (var ignored = MDC.putCloseable("correlationId", UUID.randomUUID().toString())) {
      runInternal();
    }
  }

  private void runInternal() {
    final Instant cutoff =
        ZonedDateTime.now(ZoneOffset.UTC).minusYears(draftRetentionYearsOffset).toInstant();
    final var applications =
        applicationRepository.findAll(ApplicationSpecification.findByExpiredDraft(cutoff));
    log.info("Found {} draft applications not modified since {}", applications.size(), cutoff);
    for (var application : applications) {
      try {
        applicationDataDeletionService.deleteAndRecord(
            application.getId(),
            application.getEtag(),
            DraftDataDeletionTask.class.getSimpleName());
      } catch (Exception e) {
        log.error(
            "Error while deleting data for draft application with ID {}, skipping application",
            application.getId(),
            e);
      }
    }
  }
}
