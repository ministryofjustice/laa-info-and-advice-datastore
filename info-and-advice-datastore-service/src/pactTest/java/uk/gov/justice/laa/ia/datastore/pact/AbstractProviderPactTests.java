package uk.gov.justice.laa.ia.datastore.pact;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junitsupport.State;
import java.util.Optional;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.ia.datastore.config.interceptor.UserContextInterceptor;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;
import uk.gov.justice.laa.ia.datastore.service.ApplicationService;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;
import uk.gov.justice.laa.ia.datastore.service.UserDrivenEventService;

/**
 * Base for provider verification: boots the Spring app with NO database and the service layer
 * mocked, so each {@code @State} handler just stubs {@link ApplicationService}.
 *
 * <p>{@link uk.gov.justice.laa.ia.datastore.controller.ApplicationController} is the only
 * controller in this service and depends on exactly one collaborator - {@link ApplicationService} -
 * so mocking it here replaces the real bean (and everything {@code ApplicationService} itself would
 * otherwise wire: repositories, mappers, {@code EntityManager}), meaning the context loads without
 * a real Postgres instance or Flyway migrations. {@code SystemDrivenEventService} and {@code
 * UserDrivenEventService} are also mocked below: they're independently component-scanned
 * {@code @Service}s (not dependencies reachable only via {@code ApplicationService}), so they still
 * get instantiated - and need their own {@code EventRepository}/collaborators - even when {@code
 * ApplicationService} is mocked out. {@code ApplicationRepository} and {@code EventRepository} are
 * mocked directly too: {@code DataDeletionTask}/{@code DataRetentionDateResolverTask} (scheduled
 * tasks) and {@code RetentionDateUpdateService}/{@code ApplicationDataDeletionService} are plain
 * {@code @Component}/{@code @Service} beans reachable via component scanning regardless of profile,
 * and need these repositories to construct even though nothing here exercises them. Default Spring
 * Boot/Security autoconfiguration and {@code UserContextInterceptor} (see field javadoc below) are
 * also neutralised so requests reach the controller without real JWTs.
 *
 * <p>{@code @State} handlers live here (as opposed to on {@link ApplicationProviderPactTests}
 * itself) so they're ready to be shared again if a local/offline verification path is reintroduced
 * later - one source of truth for "what a consumer gets back for each state".
 */
@EnableAutoConfiguration(
    exclude = {
      DataSourceAutoConfiguration.class,
      HibernateJpaAutoConfiguration.class,
      FlywayAutoConfiguration.class,
      DataJpaRepositoriesAutoConfiguration.class
    })
@Import(AbstractProviderPactTests.PermitAllSecurityConfig.class)
public abstract class AbstractProviderPactTests {

  @MockitoBean protected ApplicationService applicationService;
  @MockitoBean protected SystemDrivenEventService systemDrivenEventService;
  @MockitoBean protected UserDrivenEventService userDrivenEventService;
  @MockitoBean protected ApplicationRepository applicationRepository;
  @MockitoBean protected EventRepository eventRepository;
  @MockitoBean protected LockProvider lockProvider;

  // UserContextInterceptor independently requires a real, matching pair of JWTs (primary auth +
  // forwarded X-Authorization) that this harness has no way to manufacture, so it's mocked out and
  // stubbed to let every request through - consumers' pacts assert on the controller's behaviour,
  // not on this app's internal auth wiring.
  @MockitoBean protected UserContextInterceptor userContextInterceptor;

  @BeforeEach
  void allowAllRequestsThroughUserContextInterceptor() throws Exception {
    when(userContextInterceptor.preHandle(any(), any(), any())).thenReturn(true);
  }

  // ── One @State per state name ANY consumer uses ────────────────────────────
  // TODO: replace/extend with the real state names once a consumer team publishes their pact(s).

  @State("an application exists")
  public void anApplicationExists() {
    ApplicationResponse sample =
        new ApplicationResponse()
            .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .providerFirmCode("1A001L")
            .providerOfficeCode("1A001L001")
            .applicationState(ApplicationState.DRAFT);
    when(applicationService.getApplication(any())).thenReturn(Optional.of(sample));
  }

  @State("an application does not exist")
  public void anApplicationDoesNotExist() {
    when(applicationService.getApplication(any())).thenReturn(Optional.empty());
  }

  /**
   * Excluding the LAA security starter's autoconfiguration (above) leaves Spring Boot's own default
   * {@code SecurityAutoConfiguration} active, which locks every endpoint down with
   * generated-password basic auth. This permits everything instead, so the
   * {@code @TargetRequestFilter}-injected headers reach the controller unchallenged - no real auth
   * is exercised by these tests.
   */
  @TestConfiguration
  static class PermitAllSecurityConfig {

    @Bean
    SecurityFilterChain pactTestSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .csrf(csrf -> csrf.disable())
          .build();
    }
  }
}
