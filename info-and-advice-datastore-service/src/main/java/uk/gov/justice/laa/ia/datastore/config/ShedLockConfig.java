package uk.gov.justice.laa.ia.datastore.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;

/** Distributed locking for scheduled tasks, backed by the application database. */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
@ExcludeFromCodeCoverage(reason = "Config")
public class ShedLockConfig {

  /** Uses the database clock so that clock skew between instances can't affect lock expiry. */
  @Bean
  public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .usingDbTime()
            .build());
  }
}
