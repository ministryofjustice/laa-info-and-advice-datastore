package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for updating an application's status. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class UpdateApplicationIntegrationTest extends BaseIntegrationTest {

  @Test
  void shouldUpdateApplicationStatusSuccessfully() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)
                            .applicationState(ApplicationState.DRAFT)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0, "applicationState": "COMPLETED"}
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.UpdateApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final ApplicationEntity updatedApplication =
        applicationRepository.findById(applicationId).orElseThrow();

    assertThat(updatedApplication.getApplicationState()).isEqualTo(ApplicationState.COMPLETED);
    assertThat(updatedApplication.getModifiedBy()).isEqualTo("SYSTEM");
  }

  @Test
  void shouldLeaveApplicationStateUnchangedWhenNotSet() throws Exception {
    // Arrange
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)
                            .applicationState(ApplicationState.DRAFT)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0}
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.UpdateApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final ApplicationEntity updatedApplication =
        applicationRepository.findById(applicationId).orElseThrow();

    assertThat(updatedApplication.getApplicationState()).isEqualTo(ApplicationState.DRAFT);
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final String payload =
        """
        {"eTag": 0, "applicationState": "COMPLETED"}
        """;

    mockMvc
        .perform(
            patch(TestConstants.UpdateApplication, UUID.randomUUID())
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
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 99, "applicationState": "COMPLETED"}
        """;

    // Act + Assert - send with stale eTag 99 (actual is 0)
    mockMvc
        .perform(
            patch(TestConstants.UpdateApplication, applicationId)
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
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(UUID.randomUUID().toString())
                            .applicationState(ApplicationState.DRAFT)))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0, "applicationState": "COMPLETED"}
        """;

    // Act + Assert
    mockMvc
        .perform(
            patch(TestConstants.UpdateApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isForbidden());

    clearCache();
    assertThat(applicationRepository.findById(applicationId).orElseThrow().getApplicationState())
        .isEqualTo(ApplicationState.DRAFT);
  }
}
