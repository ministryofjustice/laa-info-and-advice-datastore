package uk.gov.justice.laa.ia.datastore.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.TargetRequestFilter;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Local-only smoke test for the {@code @State} handlers in {@link AbstractProviderPactTests}.
 *
 * <p>Verifies against a hand-written pact file in {@code src/pactTest/resources/local-pacts}
 * instead of the real broker, so the provider setup can be exercised end-to-end (context boot,
 * header injection, state stubbing, response matching) before broker credentials/access are
 * available.
 *
 * <p>Deliberately extends {@link AbstractProviderPactTests} directly (not {@link
 * ApplicationProviderPactTests}) so it never inherits {@code @PactBroker} - this test must work
 * with zero network/broker access.
 *
 * <p><b>Scope/limitation:</b> the pact file here is hand-written by us, not published by a real
 * consumer, so a pass only proves "our provider harness (context boot, mocking, header injection,
 * state stubbing) runs correctly end-to-end" - it does NOT prove we satisfy any actual consumer's
 * contract. That can only be confirmed once broker access is granted and {@link
 * ApplicationProviderPactTests} verifies against a real, published consumer pact.
 *
 * <p>Run with:
 *
 * <pre>./gradlew :info-and-advice-datastore-service:pactTest --tests "*LocalPactSmokeTest"</pre>
 *
 * <p>Delete this class once a real consumer pact exists and broker access is confirmed - it's a
 * bridging tool, not a permanent substitute for broker verification.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Provider("laa-info-and-advice-datastore")
@PactFolder("src/pactTest/resources/local-pacts")
public class LocalPactSmokeTest extends AbstractProviderPactTests {

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

  @TargetRequestFilter
  public void requestFilter(HttpRequest request) {
    request.setHeader("Authorization", "Bearer pact-test-token");
    request.setHeader("X-Service-Name", "pact-verification");
  }
}
