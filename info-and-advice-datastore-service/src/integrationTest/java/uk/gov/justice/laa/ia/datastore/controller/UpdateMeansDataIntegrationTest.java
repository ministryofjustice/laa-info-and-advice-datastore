package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for updating means data on an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class UpdateMeansDataIntegrationTest extends BaseIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldUpdateMeansDataSuccessfully() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {
          "eTag": 0,
          "data": {"question": "answer"},
          "result": {"status": "ELIGIBLE"}
        }
        """;

    // Act
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final JsonNode expectedData = objectMapper.readTree("{\"question\": \"answer\"}");
    final JsonNode expectedResult = objectMapper.readTree("{\"status\": \"ELIGIBLE\"}");
    final List<EligibilityResultEntity> eligibilityResults =
        eligibilityResultRepository.findAll().stream()
            .filter(result -> result.getApplicationId().equals(applicationId))
            .toList();

    assertThat(eligibilityResults).hasSize(1);
    assertThat(eligibilityResults.getFirst().getData()).isEqualTo(expectedData);
    assertThat(eligibilityResults.getFirst().getResultJson()).isEqualTo(expectedResult);
    assertThat(eligibilityResults.getFirst().getCreatedAt()).isNotNull();
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final UUID applicationId = UUID.randomUUID();
    final String payload =
        """
        {
          "eTag": 0,
          "data": {"question": "answer"},
          "result": {"status": "ELIGIBLE"}
        }
        """;
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn409_whenEtagVersionMismatch() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 99, "data": {"question": "answer"}, "result": {"status": "ELIGIBLE"}}
        """;

    // Act + Assert - send with stale eTag 99 (actual is 0)
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldReturn400_whenDataMissing() throws Exception {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final String payload =
        """
        {"eTag": 0, "result": {"status": "ELIGIBLE"}}
        """;

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn400_whenResultMissing() throws Exception {
    // Arrange
    final UUID applicationId = UUID.randomUUID();
    final String payload =
        """
        {"eTag": 0, "data": {"question": "answer"}}
        """;

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnForbidden_whenProviderOfficeCodeNotAuthorized() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(UUID.randomUUID().toString())))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0, "data": {"question": "answer"}, "result": {"status": "ELIGIBLE"}}
        """;

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isForbidden());

    clearCache();
    assertThat(
            eligibilityResultRepository.findAll().stream()
                .anyMatch(result -> result.getApplicationId().equals(applicationId)))
        .isFalse();
  }
}
