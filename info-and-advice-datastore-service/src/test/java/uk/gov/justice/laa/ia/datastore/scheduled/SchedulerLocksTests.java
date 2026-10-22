package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class SchedulerLocksTests {

  @Test
  void allDataRetentionSchedulersShareOneLockSoTheyCannotRunConcurrently() throws Exception {
    final var schedulers =
        List.of(
            LocalDataRetentionScheduler.class,
            ProdDataDeletionScheduler.class,
            LocalDataDeletionScheduler.class,
            ProdDataRetentionScheduler.class);

    for (var scheduler : schedulers) {
      final var run = scheduler.getMethod("run");
      assertEquals(
          true,
          run.isAnnotationPresent(Scheduled.class),
          scheduler.getSimpleName() + " @Scheduled");
      assertEquals(
          SchedulerLocks.DATA_RETENTION,
          run.getAnnotation(SchedulerLock.class).name(),
          scheduler.getSimpleName());
    }
  }

  @Test
  void draftDeletionSchedulersUseTheirOwnLockSoTheyDoNotBlockTheOtherJobs() throws Exception {
    final var schedulers =
        List.of(ProdDraftDataDeletionScheduler.class, LocalDraftDataDeletionScheduler.class);

    for (var scheduler : schedulers) {
      final var run = scheduler.getMethod("run");
      assertEquals(
          true,
          run.isAnnotationPresent(Scheduled.class),
          scheduler.getSimpleName() + " @Scheduled");
      assertEquals(
          SchedulerLocks.DRAFT_DATA_DELETION,
          run.getAnnotation(SchedulerLock.class).name(),
          scheduler.getSimpleName());
    }
    assertNotEquals(SchedulerLocks.DATA_RETENTION, SchedulerLocks.DRAFT_DATA_DELETION);
  }
}
