package uk.gov.justice.laa.ia.datastore.scheduled;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/** Triggers data deletion on a nightly cron for deployed environments. */
@ExcludeFromCodeCoverage(
    reason = "Just configures the scheduler, DataDeletionTask handles the actual logic")
public class ProdDataDeletionScheduler {
  private final DataDeletionTask dataDeletionTask;

  public ProdDataDeletionScheduler(DataDeletionTask dataDeletionTask) {
    this.dataDeletionTask = dataDeletionTask;
  }

  @Scheduled(cron = "${laa.datastore.data-retention.deletion-cron:0 0 1 * * *}")
  @SchedulerLock(
      name = SchedulerLocks.DATA_RETENTION,
      lockAtMostFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_MOST_FOR,
      lockAtLeastFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_LEAST_FOR)
  public void run() {
    dataDeletionTask.run();
  }
}
