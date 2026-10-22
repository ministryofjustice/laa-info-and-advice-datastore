package uk.gov.justice.laa.ia.datastore.scheduled;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/** Triggers draft application deletion on a nightly cron for deployed environments. */
@ExcludeFromCodeCoverage(
    reason = "Just configures the scheduler, DraftDataDeletionTask handles the actual logic")
public class ProdDraftDataDeletionScheduler {
  private final DraftDataDeletionTask draftDataDeletionTask;

  public ProdDraftDataDeletionScheduler(DraftDataDeletionTask draftDataDeletionTask) {
    this.draftDataDeletionTask = draftDataDeletionTask;
  }

  @Scheduled(cron = "${laa.datastore.data-retention.draft-deletion-cron:0 0 2 * * *}")
  @SchedulerLock(
      name = SchedulerLocks.DRAFT_DATA_DELETION,
      lockAtMostFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_MOST_FOR,
      lockAtLeastFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_LEAST_FOR)
  public void run() {
    draftDataDeletionTask.run();
  }
}
