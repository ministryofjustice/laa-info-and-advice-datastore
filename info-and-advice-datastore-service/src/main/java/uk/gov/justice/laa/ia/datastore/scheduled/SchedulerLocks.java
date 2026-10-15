package uk.gov.justice.laa.ia.datastore.scheduled;

/** Names and durations of the ShedLock locks used by scheduled tasks. */
public final class SchedulerLocks {
  /**
   * Shared by the retention date resolver and the data deletion task, so that only one of them runs
   * at a time across all instances.
   */
  public static final String DATA_RETENTION = "data-retention";

  /** Must exceed the longest realistic run, otherwise another instance can start mid-run. */
  public static final String DATA_RETENTION_LOCK_AT_MOST_FOR = "PT2H";

  /** Stops a fast run being repeated by another instance whose clock or schedule lags slightly. */
  public static final String DATA_RETENTION_LOCK_AT_LEAST_FOR = "PT1M";

  private SchedulerLocks() {}
}
