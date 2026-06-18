package uk.gov.justice.laa.ia.datastore.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.laa.ia.datastore.SpringBootMicroserviceApplication;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.ClientDetailsRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;

/** For shared integration test behaviours. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = SpringBootMicroserviceApplication.class)
@ContextConfiguration(initializers = PostgresContainerInitializer.class)
@ExtendWith(SpringExtension.class)
@Transactional
public abstract class BaseIntegrationTest {
  @PersistenceContext protected EntityManager entityManager;

  @Autowired protected ClientDetailsRepository clientDeatilsRepository;
  @Autowired protected ApplicationRepository applicationRepository;
  @Autowired protected EligibilityResultRepository eligibilityResultRepository;

  public void clearCache() {
    entityManager.flush();
    entityManager.clear();
  }
}
