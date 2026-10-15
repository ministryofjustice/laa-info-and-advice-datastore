package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.CreateClientCommandGenerator;
import uk.gov.justice.laa.ia.datastore.generator.StartApplicationCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for creating an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class CreateApplicationIntegrationTest extends BaseIntegrationTest {

  @Test
  void shouldCreateApplicationSuccessfully() throws Exception {
    // Arrange
    StartApplicationCommand command =
        StartApplicationCommandGenerator.create(
            builder -> builder.providerOfficeCode(PROVIDER_OFFICE_CODE));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v0/applications:start-application")
                    .withBearerWriteToken()
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(command)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.referenceNumber").exists())
            .andExpect(jsonPath("$.providerOfficeCode").exists())
            .andExpect(jsonPath("$.client").exists())
            .andExpect(jsonPath("$.client.firstName").exists())
            .andExpect(jsonPath("$.client.lastName").exists())
            .andExpect(jsonPath("$.applicationState").exists())
            .andExpect(jsonPath("$.createdBy").exists())
            .andExpect(jsonPath("$.modifiedBy").exists())
            .andReturn();

    // Assert response body fields
    String responseBody = result.getResponse().getContentAsString();
    var responseJson = objectMapper.readTree(responseBody);
    String idString = responseJson.get("id").asText();
    String startReferenceNumber = responseJson.get("referenceNumber").asText();
    UUID applicationId = UUID.fromString(idString);

    assertThat(startReferenceNumber).matches("L-\\w{3}-\\w{3}");

    // Verify the GET endpoint returns the same full model
    MvcResult getResult =
        mockMvc
            .perform(get("/api/v0/applications/{id}", applicationId).withBearerReadToken())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(idString))
            .andExpect(jsonPath("$.referenceNumber").value(startReferenceNumber))
            .andExpect(jsonPath("$.providerOfficeCode").exists())
            .andExpect(jsonPath("$.client").exists())
            .andExpect(jsonPath("$.applicationState").exists())
            .andReturn();

    // Assert all fields from start-application match GET response
    var getJson = objectMapper.readTree(getResult.getResponse().getContentAsString());
    assertThat(getJson.get("id").asText()).isEqualTo(idString);
    assertThat(getJson.get("referenceNumber").asText()).isEqualTo(startReferenceNumber);
    assertThat(getJson.get("providerOfficeCode").asText())
        .isEqualTo(responseJson.get("providerOfficeCode").asText());
    assertThat(getJson.get("applicationState").asText())
        .isEqualTo(responseJson.get("applicationState").asText());

    // Verify DB state
    clearCache();
    ApplicationEntity savedEntity = applicationRepository.findById(applicationId).orElseThrow();

    assertThat(savedEntity.getClientDetails().getFirstName())
        .isEqualTo(command.getClient().getFirstName());
    assertThat(savedEntity.getClientDetails().getLastName())
        .isEqualTo(command.getClient().getLastName());
    assertThat(savedEntity.getReferenceNumber()).isEqualTo(startReferenceNumber);
    assertThat(savedEntity.getReferenceNumber()).matches("L-\\w{3}-\\w{3}");
    assertThat(savedEntity.getCreatedBy()).isEqualTo("SYSTEM");
    assertThat(savedEntity.getProviderOfficeCode()).isEqualTo(command.getProviderOfficeCode());
  }

  @Test
  void shouldReturnForbidden_whenProviderOfficeCodeNotAuthorized() throws Exception {
    // Arrange
    StartApplicationCommand command =
        StartApplicationCommandGenerator.create(
            builder -> builder.providerOfficeCode(UUID.randomUUID().toString()));

    // Act + Assert
    mockMvc
        .perform(
            post("/api/v0/applications:start-application")
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(command)))
        .andExpect(status().isForbidden());

    assertThat(applicationRepository.findAll()).isEmpty();
  }

  @Test
  void shouldCreateApplicationSuccessfully_whenUfnProvided() throws Exception {
    // Arrange
    StartApplicationCommand command =
        StartApplicationCommandGenerator.create(
            builder -> builder.providerOfficeCode(PROVIDER_OFFICE_CODE).ufn("123456/1"));

    // Act
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v0/applications:start-application")
                    .withBearerWriteToken()
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(command)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ufn").value("123456/1"))
            .andReturn();

    // Assert DB state
    var responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
    UUID applicationId = UUID.fromString(responseJson.get("id").asText());
    clearCache();
    ApplicationEntity savedEntity = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(savedEntity.getUfn()).isEqualTo("123456/1");
  }

  @Test
  void shouldReturnConflict_whenUfnAlreadyUsedForSameOfficeCode() throws Exception {
    // Arrange
    StartApplicationCommand first =
        StartApplicationCommandGenerator.create(
            builder -> builder.providerOfficeCode(PROVIDER_OFFICE_CODE).ufn("123456/1"));
    StartApplicationCommand second =
        StartApplicationCommandGenerator.create(
            builder -> builder.providerOfficeCode(PROVIDER_OFFICE_CODE).ufn("123456/1"));

    mockMvc
        .perform(
            post("/api/v0/applications:start-application")
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(first)))
        .andExpect(status().isCreated());

    // Act + Assert
    mockMvc
        .perform(
            post("/api/v0/applications:start-application")
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(second)))
        .andExpect(status().isConflict());
  }

  @ParameterizedTest
  @ValueSource(strings = {"BG123456C", "AO123456C", "js101010D", "AB123456s"})
  void shouldReturnBadRequest_whenNiNumberInvalid(String invalidNiNumber) throws Exception {
    // Arrange
    StartApplicationCommand command =
        StartApplicationCommandGenerator.create(
            builder ->
                builder
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)
                    .client(
                        CreateClientCommandGenerator.create(
                            clientBuilder ->
                                clientBuilder.nationalInsuranceNumber(invalidNiNumber))));

    // Act + Assert
    mockMvc
        .perform(
            post("/api/v0/applications:start-application")
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(command)))
        .andExpect(status().isBadRequest());
  }
}
