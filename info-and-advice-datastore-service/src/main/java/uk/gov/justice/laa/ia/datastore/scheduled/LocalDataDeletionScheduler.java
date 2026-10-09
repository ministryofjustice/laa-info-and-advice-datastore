package uk.gov.justice.laa.ia.datastore.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/** Triggers data deletion resolution on startup and every 10 minutes for local development. */
@ExcludeFromCodeCoverage(
    reason = "Just configures the scheduler, DataDeletionTask handles the actual logic")
public class LocalDataDeletionScheduler {
  private final DataDeletionTask dataDeletionTask;

  public LocalDataDeletionScheduler(DataDeletionTask dataDeletionTask) {
    this.dataDeletionTask = dataDeletionTask;
  }

  @Scheduled(initialDelay = 0, fixedRate = 600000) // 600000 ms = 10 minutes
  public void run() {
    dataDeletionTask.run();
  }
}
