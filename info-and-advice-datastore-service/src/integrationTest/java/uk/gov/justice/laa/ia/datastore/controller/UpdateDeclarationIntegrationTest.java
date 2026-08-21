package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for updating declaration data on an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class UpdateDeclarationIntegrationTest extends BaseIntegrationTest {

  @Test
  void shouldUpdateDeclarationSuccessfully() throws Exception {
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
    final LocalDate dateSigned = LocalDate.of(2026, 8, 5);
    final String payload =
        toJson(
            DeclarationCommand.builder()
                .eTag(0L)
                .declarationConfirmation(true)
                .dateSigned(dateSigned)
                .build());

    // Act
    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    final DeclarationEntity updatedEntity =
        applicationRepository.findById(applicationId).orElseThrow().getDeclaration();

    assertThat(updatedEntity.isDeclarationConfirmation()).isTrue();
    assertThat(updatedEntity.getDateSigned()).isEqualTo(dateSigned);
  }

  @Test
  void shouldReturnBadRequest_whenDateSignedIsMissing() throws Exception {
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
    final String payload =
        """
        {"eTag": 0, "declarationConfirmation": true}
        """;

    // Act + Assert - dateSigned is required
    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnBadRequest_whenDateSignedIsInFuture() throws Exception {
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

    final String payload =
        """
        {"eTag": 0, "declarationConfirmation": true, "dateSigned": "2099-01-01"}
        """;

    // Act + Assert - dateSigned must not be in the future
    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final String payload =
        toJson(
            DeclarationCommand.builder()
                .eTag(0L)
                .declarationConfirmation(true)
                .dateSigned(java.time.LocalDate.now())
                .build());
    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, UUID.randomUUID())
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
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();
    final String payload =
        toJson(
            DeclarationCommand.builder()
                .eTag(99L)
                .declarationConfirmation(true)
                .dateSigned(java.time.LocalDate.now())
                .build());

    // Act + Assert - send with stale eTag 99 (actual is 0)
    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }

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
    final String payload =
        toJson(
            DeclarationCommand.builder()
                .eTag(0L)
                .declarationConfirmation(true)
                .dateSigned(java.time.LocalDate.now())
                .build());

    // Act + Assert
    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isForbidden());

    clearCache();
    assertThat(applicationRepository.findById(applicationId).orElseThrow().getDeclaration())
        .isNull();
  }
}
