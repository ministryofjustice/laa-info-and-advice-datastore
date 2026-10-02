package uk.gov.justice.laa.ia.datastore.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/** Triggers data retention resolution on a nightly cron for deployed environments. */
@ExcludeFromCodeCoverage(
    reason = "Just configures the scheduler, DataRetentionDateResolver handles the actual logic")
public class ProdDataRetentionScheduler {
  private final DataRetentionDateResolver dataRetentionDateResolver;

  public ProdDataRetentionScheduler(DataRetentionDateResolver dataRetentionDateResolver) {
    this.dataRetentionDateResolver = dataRetentionDateResolver;
  }

  @Scheduled(cron = "${laa.datastore.data-retention.cron:0 0 0 * * *}")
  public void run() {
    dataRetentionDateResolver.run();
  }
}
