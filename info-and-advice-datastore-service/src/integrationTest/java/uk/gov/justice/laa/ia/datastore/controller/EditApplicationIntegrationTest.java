package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
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
  void shouldUnlinkAndDeleteAddress_whenClientHasNoFixedAbode() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder ->
                                        clientBuilder.address(
                                            AddressEntityGenerator.createWithoutId(null))))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();
    final UUID addressId =
        applicationRepository
            .findById(applicationId)
            .orElseThrow()
            .getClientDetails()
            .getAddress()
            .getId();
    clearCache();

    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {
                                            "eTag": 0,
                                            "clientDetails": {"noFixedAbode": true, "address": null}
                                        }
                                        """))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getClientDetails().isNoFixedAbode()).isTrue();
    assertThat(updated.getClientDetails().getAddress()).isNull();
    assertThat(entityManager.find(AddressEntity.class, addressId)).isNull();
  }

  @Test
  void shouldApplyNullableEditsAndRecordOriginalRequestShape() throws Exception {
    JsonNode existingScopingQuestions =
        objectMapper.readTree(
            "{\"unrelatedAnswer\":\"preserve me\",\"priorLegalAidReason\":\"old reason\"}");
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder -> clientBuilder.noFixedAbode(true)))
                            .reasonForReapplication("Legacy root reason")
                            .ecfFlag(true)
                            .scopingQuestions(existingScopingQuestions)
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    final String payload =
        """
        {
          "eTag": 0,
          "reasonForReapplication": null,
          "ecfFlag": null,
          "clientDetails": {"niNumber": null},
          "scopingQuestions": {
            "priorLegalAidReason": null,
            "priorLegalAid": "same_matter"
          }
        }
        """;

    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    clearCache();
    ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getReasonForReapplication()).isEqualTo("Legacy root reason");
    assertThat(updated.getEcfFlag()).isNull();
    assertThat(updated.getClientDetails().getNiNumber()).isNull();
    assertThat(updated.getScopingQuestions().has("priorLegalAidReason")).isFalse();
    assertThat(updated.getScopingQuestions().get("unrelatedAnswer").asText())
        .isEqualTo("preserve me");
    assertThat(updated.getScopingQuestions().get("priorLegalAid").asText())
        .isEqualTo("same_matter");

    List<EventEntity> events = eventRepository.findAll();
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().getPayload()).isEqualTo(objectMapper.readTree(payload));
    assertThat(events.getFirst().getPayload().get("reasonForReapplication").isNull()).isTrue();
    assertThat(events.getFirst().getPayload().get("ecfFlag").isNull()).isTrue();
    assertThat(events.getFirst().getPayload().get("clientDetails").get("niNumber").isNull())
        .isTrue();
    assertThat(events.getFirst().getPayload().get("clientDetails").has("address")).isFalse();
  }

  @Test
  void shouldClearAndReplaceAddressFieldsWhilePreservingOmittedFields() throws Exception {
    JsonNode existingScopingQuestions =
        objectMapper.readTree("{\"unrelatedAnswer\":\"preserve me\"}");
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder ->
                                        clientBuilder
                                            .noFixedAbode(false)
                                            .address(
                                                AddressEntityGenerator.createWithoutId(
                                                    addressBuilder ->
                                                        addressBuilder
                                                            .addressLine3("Old line 3")
                                                            .county("Kent")))))
                            .reasonForReapplication("Existing root reason")
                            .scopingQuestions(existingScopingQuestions)
                            .ecfFlag(true)
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"eTag": 0, "clientDetails": {"address": {
                      "addressLine2": null,
                      "addressLine3": "New line 3"
                    }}}
                    """))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    clearCache();
    ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getReasonForReapplication()).isEqualTo("Existing root reason");
    assertThat(updated.getEcfFlag()).isTrue();
    assertThat(updated.getClientDetails().getNiNumber()).isEqualTo("AB123456Q");
    assertThat(updated.getScopingQuestions()).isEqualTo(existingScopingQuestions);
    assertThat(updated.getClientDetails().getAddress().getAddressLine2()).isNull();
    assertThat(updated.getClientDetails().getAddress().getAddressLine3()).isEqualTo("New line 3");
    assertThat(updated.getClientDetails().getAddress().getCounty()).isEqualTo("Kent");
    assertThat(updated.getClientDetails().getAddress().getAddressLine1())
        .isEqualTo("10 Downing Street");
  }

  @Test
  void shouldReplaceNullableFieldsWhenValuesAreSupplied() throws Exception {
    JsonNode existingScopingQuestions =
        objectMapper.readTree(
            "{\"unrelatedAnswer\":\"preserve me\",\"priorLegalAidReason\":\"old reason\"}");
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder -> clientBuilder.noFixedAbode(true)))
                            .reasonForReapplication("Existing root reason")
                            .ecfFlag(true)
                            .scopingQuestions(existingScopingQuestions)
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "eTag": 0,
                      "reasonForReapplication": "Updated root reason",
                      "ecfFlag": false,
                      "clientDetails": {"niNumber": "QQ123456B"},
                      "scopingQuestions": {"priorLegalAidReason": "new reason"}
                    }
                    """))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    clearCache();
    ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getReasonForReapplication()).isEqualTo("Updated root reason");
    assertThat(updated.getEcfFlag()).isFalse();
    assertThat(updated.getClientDetails().getNiNumber()).isEqualTo("QQ123456B");
    assertThat(updated.getScopingQuestions().get("priorLegalAidReason").asText())
        .isEqualTo("new reason");
    assertThat(updated.getScopingQuestions().get("unrelatedAnswer").asText())
        .isEqualTo("preserve me");
  }

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
                {
                    "eTag": 0,
                    "clientDetails": {"firstName": "Updated"},
                    "scopingQuestions": {"q1": "a1"}
                }
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
}
