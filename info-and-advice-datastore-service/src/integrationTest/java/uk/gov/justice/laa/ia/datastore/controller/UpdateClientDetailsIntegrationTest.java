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
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for updating client details on an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class UpdateClientDetailsIntegrationTest extends BaseIntegrationTest {

  @Test
  void shouldUpdateClientDetailsSuccessfully() throws Exception {
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
        {
          "eTag": 0,
          "firstName": "Jane",
          "lastName": "Doe",
          "dateOfBirth": "1985-05-20",
          "niNumber": "AB123456C",
          "noFixedAbode": false,
          "address": {
            "addressLine1": "221B Baker Street",
            "townOrCity": "London",
            "postCode": "NW1 6XE",
            "country": "GB"
          }
        }
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.UpdateClientDetails, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final ApplicationEntity updatedApplication =
        applicationRepository.findById(applicationId).orElseThrow();
    final ClientDetailsEntity clientDetails = updatedApplication.getClientDetails();

    assertThat(clientDetails.getFirstName()).isEqualTo("Jane");
    assertThat(clientDetails.getLastName()).isEqualTo("Doe");
    assertThat(clientDetails.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 5, 20));
    assertThat(clientDetails.getNiNumber()).isEqualTo("AB123456C");
    assertThat(clientDetails.isNoFixedAbode()).isFalse();

    final AddressEntity address = clientDetails.getAddress();
    assertThat(address).isNotNull();
    assertThat(address.getAddressLine1()).isEqualTo("221B Baker Street");
    assertThat(address.getTownOrCity()).isEqualTo("London");
    assertThat(address.getPostCode()).isEqualTo("NW1 6XE");
    assertThat(address.getCountry()).isEqualTo("GB");
  }

  @Test
  void shouldUpdateOnlyProvidedFieldsLeavingOthersUnchanged() throws Exception {
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
        {"eTag": 0, "firstName": "Updated"}
        """;

    // Act
    mockMvc
        .perform(
            patch(TestConstants.UpdateClientDetails, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    // Assert
    clearCache();
    final ClientDetailsEntity clientDetails =
        applicationRepository.findById(applicationId).orElseThrow().getClientDetails();
    assertThat(clientDetails.getFirstName()).isEqualTo("Updated");
    assertThat(clientDetails.getLastName()).isEqualTo("Bloggs");
    assertThat(clientDetails.getNiNumber()).isEqualTo("AB123456Q");
  }

  @Test
  void shouldReturnNotFoundWhenApplicationDoesNotExist() throws Exception {
    final String payload =
        """
        {"eTag": 0, "firstName": "Jane"}
        """;

    mockMvc
        .perform(
            patch(TestConstants.UpdateClientDetails, UUID.randomUUID())
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
        {"eTag": 99, "firstName": "Jane"}
        """;

    // Act + Assert - send with stale eTag 99 (actual is 0)
    mockMvc
        .perform(
            patch(TestConstants.UpdateClientDetails, applicationId)
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
                            .providerOfficeCode(UUID.randomUUID().toString())))
            .getId();
    clearCache();

    final String payload =
        """
        {"eTag": 0, "firstName": "Jane"}
        """;

    // Act + Assert
    mockMvc
        .perform(
            patch(TestConstants.UpdateClientDetails, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isForbidden());

    clearCache();
    assertThat(
            applicationRepository
                .findById(applicationId)
                .orElseThrow()
                .getClientDetails()
                .getFirstName())
        .isEqualTo("Joe");
  }
}
