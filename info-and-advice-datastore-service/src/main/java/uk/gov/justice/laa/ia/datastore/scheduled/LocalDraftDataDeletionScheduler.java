package uk.gov.justice.laa.ia.datastore.scheduled;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/**
 * Triggers draft application deletion every 10 minutes for local development, starting 2 minutes
 * after startup so its log output doesn't interleave with the other schedulers' initial runs.
 */
@ExcludeFromCodeCoverage(
    reason = "Just configures the scheduler, DraftDataDeletionTask handles the actual logic")
public class LocalDraftDataDeletionScheduler {
  private final DraftDataDeletionTask draftDataDeletionTask;

  public LocalDraftDataDeletionScheduler(DraftDataDeletionTask draftDataDeletionTask) {
    this.draftDataDeletionTask = draftDataDeletionTask;
  }

  @Scheduled(initialDelay = 120000, fixedRate = 600000)
  @SchedulerLock(
      name = SchedulerLocks.DRAFT_DATA_DELETION,
      lockAtMostFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_MOST_FOR,
      lockAtLeastFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_LEAST_FOR)
  public void run() {
    draftDataDeletionTask.run();
  }
}
