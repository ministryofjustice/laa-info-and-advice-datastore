package uk.gov.justice.laa.ia.datastore;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.spring.boot4.SentryProperties;
import jakarta.persistence.EntityManager;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.ia.datastore.gateway.ClaimsGateway;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.ClientDetailsRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;
import uk.gov.justice.laa.ia.datastore.repository.EvidenceRepository;

@SpringBootTest(properties = {"feature.disable-jpa-auditing=true"})
@ImportAutoConfiguration(
    exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
    })
class SpringBootMicroserviceApplicationTests {

  @Autowired private SentryProperties sentryProperties;

  @MockitoBean private EntityManager entityManager;
  @MockitoBean private ClientDetailsRepository clientDetailsRepository;
  @MockitoBean private ApplicationRepository applicationRepository;
  @MockitoBean private EligibilityResultRepository eligibilityResultRepository;
  @MockitoBean private EvidenceRepository evidenceRepository;
  @MockitoBean private EventRepository eventRepository;
  @MockitoBean private ClaimsGateway claimsGateway;
  @MockitoBean private LockProvider lockProvider;

  @Test
  void contextLoads() {
    // empty due to only testing context load

  }

  @ParameterizedTest
  @CsvSource({
    "GET /metrics, true",
    "POST /metrics, true",
    "HEAD /metrics/, true",
    "/metrics, true",
    "/metrics/, true",
    "GET /api/v0/applications, false",
    "GET /metrics-summary, false",
    "GET /metrics/details, false",
    "GET /api/metrics, false"
  })
  void ignoresOnlyMetricsTransactions(String transactionName, boolean ignored) {
    assertThat(sentryProperties.getIgnoredTransactions()).isNotNull();
    assertThat(
            sentryProperties.getIgnoredTransactions().stream()
                .anyMatch(filter -> filter.matches(transactionName)))
        .isEqualTo(ignored);
  }
}
