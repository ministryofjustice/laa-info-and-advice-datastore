package uk.gov.justice.laa.ia.datastore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.scheduled.DataRetentionDateResolver;
import uk.gov.justice.laa.ia.datastore.scheduled.LocalDataRetentionScheduler;
import uk.gov.justice.laa.ia.datastore.scheduled.ProdDataRetentionScheduler;

/** Configuration class for scheduled tasks. */
@Configuration
@EnableScheduling
public class ScheduledTasksConfig {
  @Bean
  public DataRetentionDateResolver dataRetentionDateResolver(
      ClaimsGateway claimsGateway,
      @Value("${laa.datastore.data-retention.years-offset}") int dataRetentionYearsOffset,
      ApplicationRepository applicationRepository) {
    return new DataRetentionDateResolver(
        claimsGateway, dataRetentionYearsOffset, applicationRepository);
  }

  /**
   * Bean for the production/staging/uat data retention scheduler, runs based on config defaulting
   * to midnight.
   */
  @Bean
  @Profile("!local")
  public ProdDataRetentionScheduler prodDataRetentionScheduler(
      DataRetentionDateResolver dataRetentionDateResolver) {
    return new ProdDataRetentionScheduler(dataRetentionDateResolver);
  }

  /**
   * Bean for the local development data retention scheduler, runs immediately on startup and then
   * hourly.
   */
  @Bean
  @Profile("local")
  public LocalDataRetentionScheduler localDataRetentionScheduler(
      DataRetentionDateResolver dataRetentionDateResolver) {
    return new LocalDataRetentionScheduler(dataRetentionDateResolver);
  }
}
