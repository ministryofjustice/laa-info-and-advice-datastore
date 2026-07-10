package uk.gov.justice.laa.ia.datastore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.ClientDetailsRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;

@SpringBootTest(properties = {"feature.disable-jpa-auditing=true"})
@ImportAutoConfiguration(
    exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
    })
class SpringBootMicroserviceApplicationTests {

  @MockitoBean private ClientDetailsRepository clientDetailsRepository;
  @MockitoBean private ApplicationRepository applicationRepository;
  @MockitoBean private EligibilityResultRepository eligibilityResultRepository;
  @MockitoBean private EventRepository eventRepository;

  @Test
  void contextLoads() {
    // empty due to only testing context load

  }
}
