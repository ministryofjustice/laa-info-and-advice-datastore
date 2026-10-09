package uk.gov.justice.laa.ia.datastore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.justice.laa.ia.datastore.ExcludeFromCodeCoverage;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.scheduled.DataDeletionTask;
import uk.gov.justice.laa.ia.datastore.scheduled.DataRetentionDateResolverTask;
import uk.gov.justice.laa.ia.datastore.scheduled.LocalDataDeletionScheduler;
import uk.gov.justice.laa.ia.datastore.scheduled.LocalDataRetentionScheduler;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;
import uk.gov.justice.laa.ia.datastore.service.RetentionDateUpdateService;

/** Configuration class for scheduled tasks. */
@Configuration
@EnableScheduling
@ExcludeFromCodeCoverage(reason = "Config")
public class ScheduledTasksConfig {
  @Bean
  public DataRetentionDateResolverTask dataRetentionDateResolver(
      ClaimsGateway claimsGateway,
      @Value("${laa.datastore.data-retention.years-offset}") int dataRetentionYearsOffset,
      ApplicationRepository applicationRepository,
      RetentionDateUpdateService retentionDateUpdateService) {
    return new DataRetentionDateResolverTask(
        claimsGateway, dataRetentionYearsOffset, applicationRepository, retentionDateUpdateService);
  }

  /**
   * Bean local development configuration for the data deletion task, runs immediately on startup
   * and then hourly.
   */
  @Bean
  @Profile("local")
  public DataDeletionTask dataDeletionTask(
      ApplicationRepository applicationRepository,
      ClaimsGateway claimsGateway,
      @Value("${laa.datastore.data-retention.years-offset}") int dataRetentionYearsOffset,
      RetentionDateUpdateService retentionDateUpdateService,
      ApplicationDataDeletionService applicationDataDeletionService) {
    return new DataDeletionTask(
        applicationRepository,
        claimsGateway,
        dataRetentionYearsOffset,
        retentionDateUpdateService,
        applicationDataDeletionService);
  }

  //
  // Bean for the production/staging/uat data retention scheduler, runs based on config defaulting
  // to midnight.
  //
  // @Bean
  // @Profile("!local")
  // public ProdDataRetentionScheduler prodDataRetentionScheduler(
  //     DataRetentionDateResolverTask dataRetentionDateResolver) {
  //   return new ProdDataRetentionScheduler(dataRetentionDateResolver);
  // }

  /**
   * Bean for the local development data retention scheduler, runs immediately on startup and then
   * each 10 minutes.
   */
  @Bean
  @Profile("local")
  public LocalDataRetentionScheduler localDataRetentionScheduler(
      DataRetentionDateResolverTask dataRetentionDateResolver) {
    return new LocalDataRetentionScheduler(dataRetentionDateResolver);
  }

  /**
   * Bean for the local development data deletion scheduler, runs immediately on startup and then
   * every 10 minutes.
   */
  @Bean
  @Profile("local")
  public LocalDataDeletionScheduler localDataDeletionScheduler(DataDeletionTask dataDeletionTask) {
    return new LocalDataDeletionScheduler(dataDeletionTask);
  }
}
