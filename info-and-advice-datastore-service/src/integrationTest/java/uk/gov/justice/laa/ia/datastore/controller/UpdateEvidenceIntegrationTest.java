package uk.gov.justice.laa.ia.datastore.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.TestJwtConfig;

/** Integration test for updating evidence on an application. */
public class UpdateEvidenceIntegrationTest extends BaseIntegrationTest {
  // 204
  @Test
  void shouldUpdateEvidenceSuccessfully() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .save(
                ApplicationEntityGenerator.createWithoutId(
                    builder -> {
                      builder.clientDetails(ClientDetailsEntityGenerator.createWithoutId(null));
                    }))
            .getId();
    clearCache();
    final String payload = toJson(EvidenceGenerator.createEvidenceMap());

    // Act
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .header("Authorization", "Bearer " + TestJwtConfig.WRITE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final UUID applicationId = UUID.randomUUID();
    final String payload = toJson(EvidenceGenerator.createEvidenceMap());
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .header("Authorization", "Bearer " + TestJwtConfig.WRITE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNotFound());
  }
  // 404
}
