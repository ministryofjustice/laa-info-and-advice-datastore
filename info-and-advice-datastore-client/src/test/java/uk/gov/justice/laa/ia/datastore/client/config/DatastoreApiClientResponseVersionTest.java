package uk.gov.justice.laa.ia.datastore.client.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import uk.gov.justice.laa.ia.datastore.client.api.ApplicationApi;
import uk.gov.justice.laa.ia.datastore.client.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.client.model.ApplicationResponses;
import uk.gov.justice.laa.ia.datastore.client.model.CreateClientCommand;
import uk.gov.justice.laa.ia.datastore.client.model.StartApplicationCommand;

class DatastoreApiClientResponseVersionTest {

  private static final UUID APPLICATION_ID =
      UUID.fromString("b2fc6ee3-824c-48e5-8e14-861954bcfd88");
  private final AtomicReference<String> responseBody = new AtomicReference<>();
  private final AtomicReference<String> requestBody = new AtomicReference<>();
  private HttpServer server;
  private ApplicationApi applicationApi;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(
              "POST".equals(exchange.getRequestMethod()) ? 201 : 200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();

    ClientRegistration registration =
        ClientRegistration.withRegistrationId("datastore")
            .clientId("client")
            .clientSecret("secret")
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri("http://localhost/token")
            .build();
    Instant issuedAt = Instant.now();
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "app-token", issuedAt, issuedAt.plusSeconds(60));
    OAuth2AuthorizedClient authorizedClient =
        new OAuth2AuthorizedClient(registration, "datastore-client", accessToken);
    OAuth2AuthorizedClientManager clientManager = request -> authorizedClient;
    DatastoreClientProperties properties =
        new DatastoreClientProperties(
            "http://localhost:" + server.getAddress().getPort(), "datastore");

    applicationApi =
        new DatastoreApiClientConfiguration().applicationApi(properties, clientManager);
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("applicationVersions")
  void shouldAcceptOnlySignedLongIntegerTokens(
      String description, String rawVersion, Long expectedVersion) {
    responseBody.set(applicationResponse(rawVersion));

    ApplicationResponse response =
        applicationApi.getApplication(
            APPLICATION_ID, "Bearer user-token", "correlation-id", "service");

    assertThat(response.geteTag()).isEqualTo(expectedVersion);
    assertThat(response.getCreatedAt()).isEqualTo(OffsetDateTime.parse("2026-10-02T12:00:00Z"));
    assertThat(response.getEligibilityResult().getData().getClientAge()).isEqualTo("36");
    assertThat(response.getModifiedBy()).isEqualTo("test-service");
  }

  @Test
  void shouldRetainExistingLongHandlingForOtherResponseModels() {
    responseBody.set("{\"totalElements\":\"123\"}");

    ApplicationResponses response =
        applicationApi.getApplications(
            "Bearer user-token", "correlation-id", "service", null, null, null, null, null);

    assertThat(response.getTotalElements()).isEqualTo(123L);
  }

  @Test
  void shouldUseStrictVersionsAndPreserveStartApplicationRequestSerialization() throws Exception {
    responseBody.set(applicationResponse("\"13\""));
    StartApplicationCommand command =
        new StartApplicationCommand()
            .client(
                new CreateClientCommand()
                    .firstName("Jane")
                    .lastName("Doe")
                    .dateOfBirth(java.time.LocalDate.parse("1990-01-01"))
                    .noFixedAbode(false))
            .applicationType(StartApplicationCommand.ApplicationTypeEnum.RCW)
            .providerOfficeCode("office")
            .ufn("123456789");

    ApplicationResponse response =
        applicationApi.startApplication("Bearer user-token", "correlation-id", "service", command);

    assertThat(response.geteTag()).isNull();
    assertThat(objectMapper.readTree(requestBody.get()))
        .isEqualTo(
            objectMapper.readTree(
                """
                {"client":{"firstName":"Jane","lastName":"Doe","dateOfBirth":[1990,1,1],"noFixedAbode":false},"applicationType":"RCW","providerOfficeCode":"office","ufn":"123456789"}
                """));
  }

  private static Stream<Arguments> applicationVersions() {
    return Stream.of(
        Arguments.of("zero", "0", 0L),
        Arguments.of("ordinary integer", "42", 42L),
        Arguments.of("negative integer", "-42", -42L),
        Arguments.of("above Integer maximum", "2147483648", 2147483648L),
        Arguments.of("below Integer minimum", "-2147483649", -2147483649L),
        Arguments.of("beyond floating precision", "9007199254740993", 9007199254740993L),
        Arguments.of("Long maximum", "9223372036854775807", Long.MAX_VALUE),
        Arguments.of("Long minimum", "-9223372036854775808", Long.MIN_VALUE),
        Arguments.of("missing", null, null),
        Arguments.of("null", "null", null),
        Arguments.of("numeric string", "\"42\"", null),
        Arguments.of("empty string", "\"\"", null),
        Arguments.of("boolean", "true", null),
        Arguments.of("fraction", "1.5", null),
        Arguments.of("decimal integer", "1.0", null),
        Arguments.of("exponent", "1e3", null),
        Arguments.of("overflow above Long maximum", "9223372036854775808", null),
        Arguments.of("overflow below Long minimum", "-9223372036854775809", null),
        Arguments.of("nested object", "{\"nested\":[1,{\"skip\":true}]}", null),
        Arguments.of("nested array", "[1,{\"skip\":[false]}]", null));
  }

  private static String applicationResponse(String rawVersion) {
    String versionProperty = rawVersion == null ? "" : "\"eTag\":" + rawVersion + ",";
    return """
    {
      "id":"%s",
      "individualLegalAidNumber":"b2fc6ee3-824c-48e5-8e14-861954bcfd89",
      "client":{"firstName":"Jane","lastName":"Doe","dateOfBirth":"1990-01-01","noFixedAbode":false},
      "providerFirmCode":"firm",
      "providerOfficeCode":"office",
      %s
      "createdAt":"2026-10-02T12:00:00Z",
      "createdBy":"test-user",
      "modifiedAt":"2026-10-02T12:01:00Z",
      "modifiedBy":"test-service",
      "eligibilityResult":{"data":{"client_age":"36"},"result":{"status":"eligible"}},
      "unknownAfterVersion":{"nested":[1,{"still":"parsed"}]}
    }
    """
        .formatted(APPLICATION_ID, versionProperty);
  }
}
