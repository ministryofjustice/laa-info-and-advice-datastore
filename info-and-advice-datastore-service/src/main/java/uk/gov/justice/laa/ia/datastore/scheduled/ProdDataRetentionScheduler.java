package uk.gov.justice.laa.ia.datastore.scheduled;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/** Triggers data retention resolution on a nightly cron for deployed environments. */
@ExcludeFromCodeCoverage(
    reason =
        "Just configures the scheduler, DataRetentionDateResolverTask handles the actual logic")
public class ProdDataRetentionScheduler {
  private final DataRetentionDateResolverTask dataRetentionDateResolver;

  public ProdDataRetentionScheduler(DataRetentionDateResolverTask dataRetentionDateResolver) {
    this.dataRetentionDateResolver = dataRetentionDateResolver;
  }

  @Scheduled(cron = "${laa.datastore.data-retention.cron:0 0 0 * * *}")
  @SchedulerLock(
      name = SchedulerLocks.DATA_RETENTION,
      lockAtMostFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_MOST_FOR,
      lockAtLeastFor = SchedulerLocks.DATA_RETENTION_LOCK_AT_LEAST_FOR)
  public void run() {
    dataRetentionDateResolver.run();
  }
}
