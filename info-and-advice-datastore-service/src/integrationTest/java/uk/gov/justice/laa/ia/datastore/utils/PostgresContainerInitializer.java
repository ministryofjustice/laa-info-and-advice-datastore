package uk.gov.justice.laa.ia.datastore.utils;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

/** Postgres test container setup. */
public class PostgresContainerInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  public static final String POSTGRES_INSTANCE = "postgres:18";
  private static final PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>(POSTGRES_INSTANCE);

  static {
    postgreSQLContainer.start();
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    TestPropertyValues.of(
            "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
            "spring.datasource.username=" + postgreSQLContainer.getUsername(),
            "spring.datasource.password=" + postgreSQLContainer.getPassword())
        .applyTo(applicationContext.getEnvironment());
  }
}
