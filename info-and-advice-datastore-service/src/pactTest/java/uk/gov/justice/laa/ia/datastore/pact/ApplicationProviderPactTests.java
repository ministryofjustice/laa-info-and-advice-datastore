package uk.gov.justice.laa.ia.datastore.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Provider verification entry point for {@code laa-info-and-advice-datastore}.
 *
 * <p>Pulls every consumer pact for this provider from the broker, replays each interaction against
 * the app on a random port, and (when broker creds are set) publishes results back.
 *
 * <p>Each {@code @State("...")} method is the provider-side implementation of a state a consumer
 * named with {@code .given("...")}. The string must match the consumer's literal exactly - add one
 * handler per state name as consumers are onboarded (see {@code docs/onboarding-standard.md} in
 * laa-pact-template).
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Provider("laa-info-and-advice-datastore")
@PactBroker
public class ApplicationProviderPactTests extends AbstractProviderPactTests {

  @LocalServerPort private int port;

  @BeforeEach
  void setUp(PactVerificationContext context) {
    context.setTarget(new HttpTestTarget("localhost", port));
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void pactVerificationTestTemplate(PactVerificationContext context) {
    context.verifyInteraction();
  }

  /**
   * Injects the headers consumers assert on, so verification passes without wiring real security.
   * Run with {@code feature.disable-security=true} (see {@code
   * src/pactTest/resources/application.yml}), which excludes {@code
   * SecurityFilterChainAutoConfiguration} so this header is never validated.
   */
  @TargetRequestFilter
  public void requestFilter(HttpRequest request) {
    request.setHeader("Authorization", "Bearer pact-test-token");
    request.setHeader("X-Service-Name", "pact-verification");
  }
}
