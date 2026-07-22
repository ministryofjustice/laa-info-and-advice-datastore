package uk.gov.justice.laa.ia.datastore.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for updating evidence on an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
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
                      builder
                          .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                          .providerFirmId(PROVIDER_FIRM_ID);
                    }))
            .getId();
    clearCache();
    final String payload = toJson(EvidenceGenerator.createEvidenceMap());

    // Act
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .header("If-Match", "0")
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
                .withBearerWriteToken()
                .header("If-Match", "0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNotFound());
  }

  @Test
  void shouldReturn409_whenEtagVersionMismatch() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .save(
                ApplicationEntityGenerator.createWithoutId(
                    builder -> {
                      builder
                          .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                          .providerFirmId(PROVIDER_FIRM_ID);
                    }))
            .getId();
    clearCache();
    final String payload = toJson(EvidenceGenerator.createEvidenceMap());

    // Act + Assert - send with stale version 99 (actual version is 0)
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .header("If-Match", "99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }
  // 404
}
