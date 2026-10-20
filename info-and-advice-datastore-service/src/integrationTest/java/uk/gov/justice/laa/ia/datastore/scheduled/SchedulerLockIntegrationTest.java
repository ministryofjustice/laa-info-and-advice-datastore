package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Verifies the ShedLock table and lock provider behave as the schedulers expect. */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchedulerLockIntegrationTest extends BaseIntegrationTest {

  @Autowired private LockProvider lockProvider;
  @Autowired private JdbcTemplate jdbcTemplate;

  @AfterEach
  void clearLocks() {
    jdbcTemplate.update("DELETE FROM shedlock");
  }

  @Test
  void lockIsExclusiveWhileHeld() {
    final var config =
        new LockConfiguration(
            Instant.now(),
            SchedulerLocks.DATA_RETENTION,
            Duration.ofMinutes(5),
            Duration.ofMinutes(1));

    final var first = lockProvider.lock(config);
    final var second = lockProvider.lock(config);

    assertTrue(first.isPresent());
    assertTrue(second.isEmpty());
    first.get().unlock();
    assertEquals(1, jdbcTemplate.queryForObject("SELECT count(*) FROM shedlock", Integer.class));
  }
}
