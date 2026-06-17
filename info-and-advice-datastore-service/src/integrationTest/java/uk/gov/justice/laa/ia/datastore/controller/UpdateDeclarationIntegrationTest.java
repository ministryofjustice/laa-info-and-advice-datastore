package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;

/** Integration test for updating declaration on an application. */
@WithMockUser
public class UpdateDeclarationIntegrationTest extends BaseIntegrationTest {
  @Test
  void shouldUpdateDeclarationSuccessfully() throws Exception {
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
    final String payload =
        toJson(DeclarationCommand.builder().declarationConfirmation(true).build());

    // Act
    final MvcResult result =
        mockMvc
            .perform(
                put(TestConstants.UpdateDeclaration, applicationId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isNoContent())
            .andReturn();

    // Assert
    final DeclarationEntity updatedEntity =
        applicationRepository.findById(applicationId).orElseThrow().getDeclaration();

    assertThat(updatedEntity.isDeclarationConfirmation()).isTrue();
  }
}
