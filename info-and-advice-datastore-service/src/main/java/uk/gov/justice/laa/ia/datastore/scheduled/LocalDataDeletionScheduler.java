package uk.gov.justice.laa.ia.datastore.scheduled;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/**
 * Triggers data deletion resolution every 10 minutes, offset from the retention scheduler for local
 * development.
 */
@ExcludeFromCodeCoverage(
    reason = "Just configures the scheduler, DataDeletionTask handles the actual logic")
public class LocalDataDeletionScheduler {
  private final DataDeletionTask dataDeletionTask;

  public LocalDataDeletionScheduler(DataDeletionTask dataDeletionTask) {
    this.dataDeletionTask = dataDeletionTask;
  }

  // Offset by 5 minutes from the retention scheduler so the two never fire together and contend for
  // the shared lock, which would let one job repeatedly starve the other.
  @Scheduled(initialDelay = 300000, fixedRate = 600000)
  @SchedulerLock(
      name = SchedulerLocks.DATA_RETENTION,
      lockAtMostFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_MOST_FOR,
      lockAtLeastFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_LEAST_FOR)
  public void run() {
    dataDeletionTask.run();
  }
}
