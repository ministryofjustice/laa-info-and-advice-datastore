package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.justice.laa.ia.datastore.SpringBootMicroserviceApplication;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.repository.ClientDetailsRepository;
import uk.gov.justice.laa.ia.datastore.utils.PostgresContainerInitializer;
import uk.gov.justice.laa.ia.datastore.utils.TestJwtConfig;

/** Verifies auditing with the actual request-scoped context rather than a mock. */
@ActiveProfiles("test")
@SpringBootTest(classes = SpringBootMicroserviceApplication.class)
@ContextConfiguration(initializers = PostgresContainerInitializer.class)
@Import(TestJwtConfig.class)
@Transactional
class EntityChangeRequestContextIntegrationTest {

  @Autowired private UserContext userContext;
  @Autowired private ClientDetailsRepository clientDetailsRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void usesRequestScopeWhenPresentAndWorksWithoutIt() {
    RequestAttributes previous = RequestContextHolder.getRequestAttributes();
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
    try {
      userContext.setCurrentUser("request-user");
      userContext.setCorrelationId("request-correlation");
      ClientDetailsEntity requested =
          clientDetailsRepository.saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null));
      assertThat(changeFor(requested).getUserId()).isEqualTo("request-user");
      assertThat(changeFor(requested).getCorrelationId()).isEqualTo("request-correlation");

      RequestContextHolder.resetRequestAttributes();
      ClientDetailsEntity scheduled =
          clientDetailsRepository.saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null));
      assertThat(changeFor(scheduled).getUserId()).isEqualTo("SYSTEM");
    } finally {
      RequestContextHolder.setRequestAttributes(previous);
    }
  }

  private EntityChangeEntity changeFor(ClientDetailsEntity entity) {
    return entityManager
        .createQuery(
            "select c from EntityChangeEntity c where c.entityId = :id", EntityChangeEntity.class)
        .setParameter("id", entity.getId())
        .getSingleResult();
  }
}
