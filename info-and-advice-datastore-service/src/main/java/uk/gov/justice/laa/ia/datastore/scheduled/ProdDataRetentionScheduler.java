package uk.gov.justice.laa.ia.datastore.scheduled;

import org.springframework.scheduling.annotation.Scheduled;

/** Triggers data retention resolution on a nightly cron for deployed environments. */
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
