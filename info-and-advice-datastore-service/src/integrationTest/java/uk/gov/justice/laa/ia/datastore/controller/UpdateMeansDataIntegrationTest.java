package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestJwtConfig;

/** Integration test for updating means data on an application. */
public class UpdateMeansDataIntegrationTest extends BaseIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldUpdateMeansDataSuccessfully() throws Exception {
    // Arrange
    final UUID determinationId = UUID.randomUUID();
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder.clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))))
            .getId();
    clearCache();

    final String payload =
        """
        {
          "determinationId": "%s",
          "meansAssessmentRequired": true,
          "status": "ELIGIBLE"
        }
        """
            .formatted(determinationId);

    // Act
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .header("Authorization", "Bearer " + TestJwtConfig.WRITE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk());

    // Assert
    clearCache();
    final ApplicationEntity updatedApplication =
        applicationRepository.findById(applicationId).orElseThrow();
    final JsonNode expectedJson = objectMapper.readTree(payload);
    final List<EligibilityResultEntity> eligibilityResults =
        eligibilityResultRepository.findAll().stream()
            .filter(result -> result.getApplicationId().equals(applicationId))
            .toList();

    assertThat(updatedApplication.getDeterminationId()).isEqualTo(determinationId);
    assertThat(updatedApplication.getMeansAssessmentRequired()).isTrue();

    assertThat(eligibilityResults).hasSize(1);
    assertThat(eligibilityResults.getFirst().getResultJson()).isEqualTo(expectedJson);
    assertThat(eligibilityResults.getFirst().getCreatedAt()).isNotNull();
  }
}
