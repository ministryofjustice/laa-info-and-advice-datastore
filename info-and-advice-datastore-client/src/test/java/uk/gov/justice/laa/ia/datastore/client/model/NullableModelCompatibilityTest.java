package uk.gov.justice.laa.ia.datastore.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import uk.gov.justice.laa.ia.datastore.client.config.DatastoreApiClientConfiguration;
import uk.gov.justice.laa.ia.datastore.client.config.DatastoreClientProperties;

class NullableModelCompatibilityTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JsonNullableModule());

  @Test
  void shouldRetainCreateClientModelTypesAndRepresentScopingNullability() throws Exception {
    assertThat(UpdateClientDetailsCommand.class.getMethod("getNiNumber").getReturnType())
        .isEqualTo(String.class);
    assertThat(UpdateClientDetailsCommand.class.getMethod("getAddress").getReturnType())
        .isEqualTo(UpdateAddressCommand.class);
    assertThat(UpdateAddressCommand.class.getMethod("getAddressLine2").getReturnType())
        .isEqualTo(String.class);

    UpdateScopingDataCommand omitted =
        objectMapper.readValue("{\"eTag\":0}", UpdateScopingDataCommand.class);
    UpdateScopingDataCommand cleared =
        objectMapper.readValue(
            "{\"eTag\":0,\"scopingQuestions\":null}", UpdateScopingDataCommand.class);
    UpdateScopingDataCommand supplied =
        objectMapper.readValue(
            "{\"eTag\":0,\"scopingQuestions\":{\"priorLegalAid\":\"same_matter\"}}",
            UpdateScopingDataCommand.class);

    assertThat(omitted.getScopingQuestions_JsonNullable().isPresent()).isFalse();
    assertThat(cleared.getScopingQuestions_JsonNullable().isPresent()).isTrue();
    assertThat(cleared.getScopingQuestions()).isNull();
    assertThat(supplied.getScopingQuestions_JsonNullable().get())
        .containsEntry("priorLegalAid", "same_matter");

    String clearedJson = objectMapper.writeValueAsString(cleared);
    assertThat(objectMapper.readTree(clearedJson).get("scopingQuestions").isNull()).isTrue();
    assertThat(objectMapper.writeValueAsString(omitted)).doesNotContain("scopingQuestions");

    String suppliedJson = objectMapper.writeValueAsString(supplied);
    assertThat(objectMapper.readTree(suppliedJson).get("scopingQuestions").get("priorLegalAid"))
        .isEqualTo(objectMapper.getNodeFactory().textNode("same_matter"));
    assertThat(suppliedJson).doesNotContain("present", "value");
  }

  @Test
  void shouldOmitUnspecifiedFieldsAndSerializeNullForFieldsToClear() throws Exception {
    AtomicReference<String> requestBody = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          requestBody.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          exchange.sendResponseHeaders(204, -1);
          exchange.close();
        });
    server.start();

    try {
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

      new DatastoreApiClientConfiguration()
          .applicationApi(properties, clientManager)
          .editApplication(
              UUID.randomUUID(),
              "Bearer user-token",
              "correlation-id",
              "service-name",
              new EditApplicationCommand()
                  .eTag(0L)
                  .clientDetails(new PatchClientDetailsData().firstName("Jane").niNumber(null)));

      assertThat(objectMapper.readTree(requestBody.get()))
          .isEqualTo(
              objectMapper.readTree(
                  """
                  {"eTag":0,"clientDetails":{"firstName":"Jane","niNumber":null}}
                  """));
    } finally {
      server.stop(0);
    }
  }
}
