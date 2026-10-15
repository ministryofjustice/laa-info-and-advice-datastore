package uk.gov.justice.laa.ia.datastore.scheduled;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/** Triggers data retention resolution on startup and every 10 minutes for local development. */
@ExcludeFromCodeCoverage(
    reason =
        "Just configures the scheduler, DataRetentionDateResolverTask handles the actual logic")
public class LocalDataRetentionScheduler {
  private final DataRetentionDateResolverTask dataRetentionDateResolver;

  public LocalDataRetentionScheduler(DataRetentionDateResolverTask dataRetentionDateResolver) {
    this.dataRetentionDateResolver = dataRetentionDateResolver;
  }

  @Scheduled(initialDelay = 0, fixedRate = 600000) // 600000 ms = 10 minutes
  @SchedulerLock(
      name = SchedulerLocks.DATA_RETENTION,
      lockAtMostFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_MOST_FOR,
      lockAtLeastFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_LEAST_FOR)
  public void run() {
    dataRetentionDateResolver.run();
  }
}
