package uk.gov.justice.laa.ia.datastore.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.SpringBootMicroserviceApplication;
import uk.gov.justice.laa.ia.datastore.config.interceptor.UserContextInterceptor;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.ClientDetailsRepository;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;

/** For shared integration test behaviours. */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(classes = SpringBootMicroserviceApplication.class)
@ContextConfiguration(initializers = PostgresContainerInitializer.class)
@ExtendWith(SpringExtension.class)
@Import(TestJwtConfig.class)
@Transactional
public abstract class BaseIntegrationTest {
  @PersistenceContext protected EntityManager entityManager;

  @Autowired protected ClientDetailsRepository clientDetailsRepository;
  @Autowired protected ApplicationRepository applicationRepository;
  @Autowired protected EligibilityResultRepository eligibilityResultRepository;
  @Autowired protected EventRepository eventRepository;
  @Autowired protected MockMvc mockMvc;
  protected final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  @MockitoBean private UserContextInterceptor userContextInterceptor;
  @MockitoBean private UserContext userContext;
  protected static final String FIRM_CODE = "123456";
  protected static final String PROVIDER_OFFICE_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() throws Exception {
    when(userContextInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    when(userContext.getProviderFirmCode()).thenReturn(FIRM_CODE);
    when(userContext.getProviderOfficeId()).thenReturn(PROVIDER_OFFICE_ID);
    when(userContext.getCurrentUser()).thenReturn("SYSTEM");
  }

  public void clearCache() {
    entityManager.flush();
    entityManager.clear();
  }

  protected String toJson(Object object) throws Exception {
    return objectMapper.writeValueAsString(object);
  }
}
