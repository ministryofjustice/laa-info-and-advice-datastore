package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for updating means data on an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class UpdateMeansDataIntegrationTest extends BaseIntegrationTest {

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
          "data": {"client_age": "18-24"},
          "result": {
            "legacy_answer": "retained",
            "version": "6",
            "success": true,
            "result_summary": {
              "overall_result": {"result": "eligible"},
              "gross_income": {"total_gross_income": 500.25},
              "capital": {"combined_assessed_capital": 0, "proceeding_types": []}
            },
            "assessment": {
              "submission_date": "2026-01-01",
              "client_reference_id": null,
              "applicant": {"employed": null},
              "gross_income": {
                "employment_income": [],
                "self_employment_details": [
                  {"monthly_income": {"gross": 500.25, "tax": -25}}
                ]
              },
              "capital": {"capital_items": {"properties": {"main_home": null}}},
              "remarks": {}
            }
          }
        }
        """;

    // Act
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final JsonNode expectedResult = objectMapper.readTree(payload).get("result");
    final List<EligibilityAssessmentEntity> eligibilityAssessments =
        eligibilityAssessmentRepository.findAll().stream()
            .filter(result -> result.getApplicationId().equals(applicationId))
            .toList();

    assertThat(eligibilityAssessments).hasSize(1);
    assertThat(eligibilityAssessments.getFirst().getData().get("client_age").asText())
        .isEqualTo("18-24");
    assertThat(eligibilityAssessments.getFirst().getResultJson()).isEqualTo(expectedResult);
    assertThat(eligibilityAssessments.getFirst().getIndication()).isTrue();
    assertThat(eligibilityAssessments.getFirst().getCreatedAt()).isNotNull();
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final UUID applicationId = UUID.randomUUID();
    final String payload =
        """
        {
          "eTag": 0,
          "data": {"client_age": "18-24"},
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
        {"eTag": 99, "data": {"client_age": "18-24"}, "result": {"status": "ELIGIBLE"}}
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
        {"eTag": 0, "data": {"client_age": "18-24"}}
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
        {"eTag": 0, "data": {"client_age": "18-24"}, "result": {"status": "ELIGIBLE"}}
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
            eligibilityAssessmentRepository.findAll().stream()
                .anyMatch(result -> result.getApplicationId().equals(applicationId)))
        .isFalse();
  }

  @Test
  void shouldReturnGone_whenApplicationIsDeleted() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)
                            .deleted(true)))
            .getId();
    clearCache();

    final String payload =
        """
        {
          "eTag": 0,
          "data": {"client_age": "18-24"},
          "result": {"status": "ELIGIBLE"}
        }
        """;

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isGone());
  }
}
