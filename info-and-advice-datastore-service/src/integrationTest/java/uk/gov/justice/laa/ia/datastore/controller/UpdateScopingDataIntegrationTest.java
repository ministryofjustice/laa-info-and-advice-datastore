package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for updating scoping data on an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class UpdateScopingDataIntegrationTest extends BaseIntegrationTest {

  @Test
  void shouldUpdateScopingDataSuccessfully() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {
          "eTag": 0,
          "scopingQuestions": {
            "priorLegalAid": "same_matter",
            "priorLegalAidReason": "Previous matter was related"
          }
        }
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.UpdateScopingData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());

    // Assert
    clearCache();
    final ApplicationEntity updatedApplication =
        applicationRepository.findById(applicationId).orElseThrow();

    assertThat(updatedApplication.getScopingQuestions()).isNotNull();
    assertThat(updatedApplication.getScopingQuestions().get("priorLegalAid").asText())
        .isEqualTo("same_matter");
    assertThat(updatedApplication.getScopingQuestions().get("priorLegalAidReason").asText())
        .isEqualTo("Previous matter was related");
  }

  @Test
  void shouldUpdateScopingDataWithNullScopingQuestions() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0, "scopingQuestions": null}
        """;

    // Act + Assert
    mockMvc
        .perform(
            patch(TestConstants.UpdateScopingData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final String payload =
        """
        {"eTag": 0, "scopingQuestions": {"priorLegalAid": "same_matter"}}
        """;

    mockMvc
        .perform(
            patch(TestConstants.UpdateScopingData, UUID.randomUUID())
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
                            .providerFirmCode(FIRM_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 99, "scopingQuestions": {"priorLegalAid": "different_matter"}}
        """;

    // Act + Assert - send with stale eTag 99 (actual is 0)
    mockMvc
        .perform(
            patch(TestConstants.UpdateScopingData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }
}
