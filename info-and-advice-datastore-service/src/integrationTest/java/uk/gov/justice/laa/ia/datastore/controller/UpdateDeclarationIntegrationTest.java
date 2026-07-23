package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for updating declaration on an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class UpdateDeclarationIntegrationTest extends BaseIntegrationTest {
  @Test
  void shouldUpdateDeclarationSuccessfully() throws Exception {
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
    final String payload =
        toJson(DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build());

    // Act
    final MvcResult result =
        mockMvc
            .perform(
                put(TestConstants.UpdateDeclaration, applicationId)
                    .withBearerWriteToken()
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isNoContent())
            .andReturn();

    // Assert
    final DeclarationEntity updatedEntity =
        applicationRepository.findById(applicationId).orElseThrow().getDeclaration();

    assertThat(updatedEntity.isDeclarationConfirmation()).isTrue();
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final UUID applicationId = UUID.randomUUID();
    final String payload =
        toJson(DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build());
    mockMvc
        .perform(
            put(TestConstants.UpdateDeclaration, applicationId)
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
                          .providerFirmId(PROVIDER_FIRM_ID);
                    }))
            .getId();
    clearCache();
    final String payload =
        toJson(DeclarationCommand.builder().eTag(99L).declarationConfirmation(true).build());

    // Act + Assert - send with stale eTag 99 (actual is 0)
    mockMvc
        .perform(
            put(TestConstants.UpdateDeclaration, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isConflict());
  }
}
