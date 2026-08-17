package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                          .providerFirmCode(FIRM_CODE)
                          .providerOfficeCode(PROVIDER_OFFICE_CODE);
                    }))
            .getId();
    clearCache();
    final String payload = toJson(EvidenceGenerator.createUpdateEvidenceCommand(0L));

    // Act
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  void shouldReturnEvidenceChecklistsAsProperJsonOnGet() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .save(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();
    final String payload = toJson(EvidenceGenerator.createUpdateEvidenceCommand(0L));

    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());
    clearCache();

    // Act & Assert: the checklist payloads must round-trip as plain JSON, not as
    // serialized JsonNode bean properties (e.g. "object", "nodeType", "containerNode").
    mockMvc
        .perform(get("/api/v0/applications/{id}", applicationId).withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.evidence.incomeEvidenceChecklist.wageSlips").value(true))
        .andExpect(
            jsonPath("$.evidence.expenditureCapitalEvidenceChecklist.bankStatements").value(true));
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final UUID applicationId = UUID.randomUUID();
    final String payload = toJson(EvidenceGenerator.createUpdateEvidenceCommand(0L));
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
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
            .save(
                ApplicationEntityGenerator.createWithoutId(
                    builder -> {
                      builder
                          .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                          .providerFirmCode(FIRM_CODE)
                          .providerOfficeCode(PROVIDER_OFFICE_CODE);
                    }))
            .getId();
    clearCache();
    final String payload = toJson(EvidenceGenerator.createUpdateEvidenceCommand(99L));

    // Act + Assert - send with stale eTag 99 (actual is 0)
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }

  // 404

  @Test
  void shouldReturnForbidden_whenProviderOfficeCodeNotAuthorized() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .save(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(UUID.randomUUID().toString())))
            .getId();
    clearCache();
    final String payload = toJson(EvidenceGenerator.createUpdateEvidenceCommand(0L));

    // Act + Assert
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isForbidden());

    clearCache();
    assertThat(applicationRepository.findById(applicationId).orElseThrow().getEvidence()).isNull();
  }
}
