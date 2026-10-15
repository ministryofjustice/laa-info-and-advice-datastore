package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/**
 * Starts two instances of the real scheduler at the same moment, standing in for two pods, with
 * tasks that block while running, and checks only one task runs because of the shared lock.
 */
// Two scheduler threads stand in for two pods; with the default single thread the tasks would
// queue.
@TestPropertySource(properties = "spring.task.scheduling.pool.size=2")
@Import(SchedulerLockFiringIntegrationTest.SchedulersConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchedulerLockFiringIntegrationTest extends BaseIntegrationTest {
  private static final AtomicInteger INVOCATIONS = new AtomicInteger();
  private static final CountDownLatch FIRST_STARTED = new CountDownLatch(1);
  private static final CountDownLatch RELEASE = new CountDownLatch(1);

  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void clearLocks() {
    RELEASE.countDown();
    jdbcTemplate.update("DELETE FROM shedlock");
  }

  @Test
  void onlyOneInstanceRunsTheTaskWhenBothSchedulersFireAtOnce() throws Exception {
    assertTrue(FIRST_STARTED.await(10, TimeUnit.SECONDS), "a scheduled task should have started");
    TimeUnit.SECONDS.sleep(2);

    assertEquals(1, INVOCATIONS.get());
  }

  @TestConfiguration
  static class SchedulersConfig {
    @Bean
    LocalDataRetentionScheduler firstPodRetentionScheduler() {
      final var task = mock(DataRetentionDateResolverTask.class);
      doAnswer(invocation -> blockWhileRunning()).when(task).run();
      return new LocalDataRetentionScheduler(task);
    }

    @Bean
    LocalDataRetentionScheduler secondPodRetentionScheduler() {
      final var task = mock(DataRetentionDateResolverTask.class);
      doAnswer(invocation -> blockWhileRunning()).when(task).run();
      return new LocalDataRetentionScheduler(task);
    }

    private static Object blockWhileRunning() throws InterruptedException {
      INVOCATIONS.incrementAndGet();
      FIRST_STARTED.countDown();
      RELEASE.await(30, TimeUnit.SECONDS);
      return null;
    }
  }
}
