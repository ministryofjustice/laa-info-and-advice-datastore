package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                            .providerFirmCode(FIRM_CODE)))
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
        .andExpect(status().isNoContent());

    // Assert
    final DeclarationEntity updatedEntity =
        applicationRepository.findById(applicationId).orElseThrow().getDeclaration();

    assertThat(updatedEntity.isDeclarationConfirmation()).isTrue();
    assertThat(updatedEntity.getDateSigned()).isEqualTo(dateSigned);
  }

  @Test
  void shouldUpdateDeclarationWithoutDateSigned() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .save(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)))
            .getId();
    clearCache();
    final String payload =
        toJson(DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build());

    // Act
    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());

    // Assert
    final DeclarationEntity updatedEntity =
        applicationRepository.findById(applicationId).orElseThrow().getDeclaration();

    assertThat(updatedEntity.isDeclarationConfirmation()).isTrue();
    assertThat(updatedEntity.getDateSigned()).isNull();
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final String payload =
        toJson(DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build());
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
                            .providerFirmCode(FIRM_CODE)))
            .getId();
    clearCache();
    final String payload =
        toJson(DeclarationCommand.builder().eTag(99L).declarationConfirmation(true).build());

    // Act + Assert - send with stale eTag 99 (actual is 0)
    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }
}
