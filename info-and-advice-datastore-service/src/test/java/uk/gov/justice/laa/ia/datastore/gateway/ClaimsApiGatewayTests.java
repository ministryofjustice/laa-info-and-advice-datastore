package uk.gov.justice.laa.ia.datastore.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.ia.datastore.config.ClaimsApiProperties;

class ClaimsApiGatewayTests {
  private static final String API_KEY = "secret-key";
  private static final UUID CLAIM_1 = UUID.randomUUID();
  private static final UUID CLAIM_2 = UUID.randomUUID();

  private HttpServer server;
  private final List<String> requestUris = new ArrayList<>();
  private final List<String> authHeaders = new ArrayList<>();
  private int errorStatus = 0;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/api/v1/claims/retention-lookup",
        exchange -> {
          requestUris.add(exchange.getRequestURI().toString());
          authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
          final var query = exchange.getRequestURI().getQuery();
          final var body = errorStatus != 0 ? "{}" : pageBody(query.contains("page=1") ? 1 : 0);
          final var bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(errorStatus != 0 ? errorStatus : 200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void readsEveryPageAndMapsSnakeCaseFields() {
    final var response = gateway().getClaims("0R695K", "230493/001");

    assertEquals(2, response.getClaims().size());
    final var first = response.getClaims().get(0);
    assertEquals(CLAIM_1, first.getClaimId());
    assertEquals("VALID", first.getStatus());
    assertEquals(OffsetDateTime.parse("2024-01-02T03:04:05Z"), first.getCreatedOn());
    assertEquals(OffsetDateTime.parse("2024-02-03T04:05:06Z"), first.getUpdatedOn());
    assertEquals(CLAIM_2, response.getClaims().get(1).getClaimId());
    assertEquals(2, requestUris.size());
  }

  @Test
  void sendsOfficeCodeUfnPagingAndApiKey() {
    gateway().getClaims("0R695K", "230493/001");

    final var firstRequest = requestUris.get(0);
    assertTrue(firstRequest.contains("office_code=0R695K"), firstRequest);
    assertTrue(firstRequest.contains("ufn=230493%2F001"), firstRequest);
    assertTrue(firstRequest.contains("page=0"), firstRequest);
    assertTrue(firstRequest.contains("size=50"), firstRequest);
    assertTrue(requestUris.get(1).contains("page=1"));
    assertEquals(List.of(API_KEY, API_KEY), authHeaders);
  }

  @Test
  void propagatesServerErrorsSoTheCallerCanSkipTheApplication() {
    errorStatus = 500;

    assertThrows(HttpServerErrorException.class, () -> gateway().getClaims("0R695K", "230493/001"));
  }

  private ClaimsApiGateway gateway() {
    final var properties =
        new ClaimsApiProperties(
            ClaimsApiProperties.LIVE_MODE,
            "http://localhost:" + server.getAddress().getPort(),
            API_KEY,
            50,
            Duration.ofSeconds(2),
            Duration.ofSeconds(5));
    return new ClaimsApiGateway(RestClient.builder(), properties);
  }

  private static String pageBody(int page) {
    final var claim = page == 0 ? CLAIM_1 : CLAIM_2;
    return """
        {"content":[{"claim_id":"%s","status":"VALID",\
        "created_on":"2024-01-02T03:04:05Z","updated_on":"2024-02-03T04:05:06Z",\
        "ignored":"x"}],\
        "total_pages":2,"total_elements":2,"number":%d,"size":1}"""
        .formatted(claim, page);
  }
}
