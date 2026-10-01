package uk.gov.justice.laa.ia.datastore.scheduled;

import org.springframework.scheduling.annotation.Scheduled;

/** Triggers data retention resolution on startup and hourly for local development. */
public class LocalDataRetentionScheduler {
  private final DataRetentionDateResolver dataRetentionDateResolver;

  public LocalDataRetentionScheduler(DataRetentionDateResolver dataRetentionDateResolver) {
    this.dataRetentionDateResolver = dataRetentionDateResolver;
  }

  @Scheduled(initialDelay = 0, fixedRate = 3_600_000)
  public void run() {
    dataRetentionDateResolver.run();
  }
}
