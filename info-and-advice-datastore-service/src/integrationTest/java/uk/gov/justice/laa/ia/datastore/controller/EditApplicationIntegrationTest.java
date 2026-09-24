package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ClientDeclarationStatus;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/**
 * Integration test for editing an application, including the linked client details, declaration and
 * evidence entities via the same PATCH endpoint.
 */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class EditApplicationIntegrationTest extends BaseIntegrationTest {

  @Test
  void shouldPatchClientDetailsAndScopingQuestions() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder -> clientBuilder.firstName("Original")))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0, "clientDetails": {"firstName": "Updated"}, "scopingQuestions": {"q1": "a1"}}
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getClientDetails().getFirstName()).isEqualTo("Updated");
    assertThat(updated.getScopingQuestions().get("q1").asText()).isEqualTo("a1");
  }

  @Test
  void shouldPatchExistingDeclaration() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .declaration(DeclarationEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0, "declaration": {"declarationConfirmation": true}}
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getDeclaration().isDeclarationConfirmation()).isTrue();
  }

  @Test
  void shouldReturn409_whenPatchingAlreadySignedDeclaration() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .declaration(
                                DeclarationEntityGenerator.createWithoutId(
                                    declarationBuilder ->
                                        declarationBuilder.dateSigned(java.time.LocalDate.now())))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0, "declaration": {"declarationConfirmation": true}}
        """;

    // Act + Assert
    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }

  @Test
  void shouldCreateDeclaration_whenNoneExistsYet() throws Exception {
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
        {"eTag": 0, "declaration": {"declarationConfirmation": true}}
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    final DeclarationEntity declaration = updated.getDeclaration();
    assertThat(declaration).isNotNull();
    assertThat(declaration.isDeclarationConfirmation()).isTrue();
    assertThat(declaration.getClientDeclarationStatus()).isEqualTo(ClientDeclarationStatus.DRAFT);
  }

  @Test
  void shouldCreateEvidence_whenNoneExistsYet() throws Exception {
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
        {"eTag": 0, "evidence": {"evidenceExemptionCode": "EXEMPT_01"}}
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    final EvidenceEntity evidence = updated.getEvidence();
    assertThat(evidence).isNotNull();
    assertThat(evidence.getEvidenceExemptionCode()).isEqualTo("EXEMPT_01");
  }

  @ParameterizedTest
  @ValueSource(strings = {"BG123456C", "AO123456C", "js101010D", "AB123456s"})
  void shouldReturnBadRequest_whenNiNumberInvalid(String invalidNiNumber) throws Exception {
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
        {"eTag": 0, "clientDetails": {"niNumber": "%s"}}
        """
            .formatted(invalidNiNumber);

    // Act + Assert
    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }
}
