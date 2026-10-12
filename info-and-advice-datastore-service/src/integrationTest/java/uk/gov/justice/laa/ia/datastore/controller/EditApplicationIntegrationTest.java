package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
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
  void shouldRejectEditOfCompletedApplicationBeforeCheckingEtag() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)
                            .applicationState(ApplicationState.COMPLETED)
                            .laaReference("original")))
            .getId();
    clearCache();

    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eTag\":999,\"laaReference\":\"changed\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("Completed applications cannot be edited."))
        .andExpect(jsonPath("$.reason").value("APPLICATION_COMPLETED"));

    clearCache();
    final ApplicationEntity unchanged = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(unchanged.getApplicationState()).isEqualTo(ApplicationState.COMPLETED);
    assertThat(unchanged.getLaaReference()).isEqualTo("original");
    assertThat(unchanged.getEtag()).isZero();
    assertThat(eventRepository.findAll()).isEmpty();
  }

  @Test
  void shouldUnlinkAndDeleteAddress_whenAddressIsExplicitlyCleared() throws Exception {
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
  void shouldCreateAddress_whenClientHasNoAddress() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder -> clientBuilder.noFixedAbode(false)))
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
                        "addressLine1": "4 Example Street",
                        "country": "GB"
                    }}}
                    """))
        .andExpect(status().isNoContent())
        .andExpect(header().string("ETag", "\"1\""));

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getEtag()).isEqualTo(1L);
    final AddressEntity address = updated.getClientDetails().getAddress();
    assertThat(address).isNotNull();
    assertThat(address.getId()).isNotNull();
    assertThat(address.getAddressLine1()).isEqualTo("4 Example Street");
    assertThat(address.getCountry()).isEqualTo("GB");
    assertThat(entityManager.find(AddressEntity.class, address.getId())).isNotNull();
  }

  @Test
  void shouldAllowCorrespondenceAddressWhenClientHasNoFixedAbode() throws Exception {
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
                                            .firstName("Original")
                                            .noFixedAbode(false)
                                            .address(AddressEntityGenerator.createWithoutId(null))))
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
                    {"eTag":0,"clientDetails":{"firstName":"Changed","noFixedAbode":true,
                      "address":{"addressLine1":"4 Example Street","country":"GB"}}}
                    """))
        .andExpect(status().isNoContent())
        .andExpect(header().string("ETag", "\"1\""));

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getEtag()).isEqualTo(1L);
    assertThat(updated.getClientDetails().getFirstName()).isEqualTo("Changed");
    assertThat(updated.getClientDetails().isNoFixedAbode()).isTrue();
    assertThat(updated.getClientDetails().getAddress().getAddressLine1())
        .isEqualTo("4 Example Street");
    assertThat(eventRepository.findAll()).hasSize(1);
  }

  @Test
  void shouldRejectAddressCreationWithoutRequiredFields() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder -> clientBuilder.noFixedAbode(false)))
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
                      "addressLine1": "4 Example Street"
                    }}}
                    """))
        .andExpect(status().isBadRequest());

    clearCache();
    final ApplicationEntity unchanged = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(unchanged.getEtag()).isZero();
    assertThat(unchanged.getClientDetails().getAddress()).isNull();
    assertThat(eventRepository.findAll()).isEmpty();
  }

  @Test
  void shouldPatchNameBirthDateAndEmptyStringsWithoutAddress() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder -> clientBuilder.noFixedAbode(true)))
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
                      "eTag":0,
                      "clientDetails":{
                        "firstName":"",
                        "dateOfBirth":"2000-02-29",
                        "niNumber":"AB123456C"
                      }
                    }
                    """))
        .andExpect(status().isNoContent());

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getClientDetails().getFirstName()).isEmpty();
    assertThat(updated.getClientDetails().getDateOfBirth())
        .isEqualTo(java.time.LocalDate.of(2000, 2, 29));
    assertThat(updated.getClientDetails().getNiNumber()).isEqualTo("AB123456C");
    assertThat(updated.getClientDetails().getAddress()).isNull();
  }

  @Test
  void shouldPreserveExistingAddressWhenAddressPropertyIsOmitted() throws Exception {
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
                                                            .addressLine1("Existing Street")
                                                            .country("GB")
                                                            .county("Kent")))))
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

    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "eTag":0,
                        "clientDetails": {
                            "firstName":"Jane",
                            "noFixedAbode":true
                        }
                    }
                    """))
        .andExpect(status().isNoContent());

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getClientDetails().getFirstName()).isEqualTo("Jane");
    assertThat(updated.getClientDetails().getNiNumber()).isEqualTo("AB123456Q");
    assertThat(updated.getClientDetails().isNoFixedAbode()).isTrue();
    assertThat(updated.getClientDetails().getAddress().getId()).isEqualTo(addressId);
    assertThat(updated.getClientDetails().getAddress().getCounty()).isEqualTo("Kent");
    assertThat(entityManager.find(AddressEntity.class, addressId)).isNotNull();
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
                            .reasonForReapplication("Legacy ReapplicationReason")
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
    assertThat(updated.getReasonForReapplication()).isNull();
    assertThat(updated.getEcfFlag()).isNull();
    assertThat(updated.getClientDetails().getNiNumber()).isNull();
    assertThat(updated.getScopingQuestions().has("priorLegalAidReason")).isFalse();
    assertThat(updated.getScopingQuestions().get("unrelatedAnswer").asText())
        .isEqualTo("preserve me");
    assertThat(updated.getScopingQuestions().get("priorLegalAid").asText())
        .isEqualTo("same_matter");

    List<EventEntity> events = eventRepository.findAll();
    assertThat(events).hasSize(1);
    EventEntity event = events.getFirst();
    assertThat(event.getPayload().get("reasonForReapplication").isNull()).isTrue();
    assertThat(event.getPayload().get("ecfFlag").isNull()).isTrue();
    // niNumber is a configured PII field, so explicit null is redacted to the nil UUID constant.
    assertThat(event.getPayload().get("clientDetails").get("niNumber").asText())
        .isEqualTo("00000000-0000-0000-0000-000000000000");
    assertThat(event.getPayload().get("clientDetails").has("address")).isFalse();
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
                            .reasonForReapplication("Existing ReapplicationReason")
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
    assertThat(updated.getReasonForReapplication()).isEqualTo("Existing ReapplicationReason");
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
  void shouldPersistNullForEveryNullableAddressField() throws Exception {
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
                                                            .addressLine1("Existing Street")
                                                            .country("GB")
                                                            .addressLine2("Flat 2")
                                                            .addressLine3("Line 3")
                                                            .addressLine4("Line 4")
                                                            .townOrCity("London")
                                                            .postCode("SW1A 1AA")
                                                            .county("Kent")))))
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
                    {"eTag":0,"clientDetails":{"address":{
                      "addressLine2":null,"addressLine3":null,"addressLine4":null,
                      "townOrCity":null,"postCode":null,"county":null
                    }}}
                    """))
        .andExpect(status().isNoContent());

    clearCache();
    final AddressEntity address =
        applicationRepository.findById(applicationId).orElseThrow().getClientDetails().getAddress();
    assertThat(address.getAddressLine2()).isNull();
    assertThat(address.getAddressLine3()).isNull();
    assertThat(address.getAddressLine4()).isNull();
    assertThat(address.getTownOrCity()).isNull();
    assertThat(address.getPostCode()).isNull();
    assertThat(address.getCounty()).isNull();
    assertThat(address.getAddressLine1()).isEqualTo("Existing Street");
    assertThat(address.getCountry()).isEqualTo("GB");
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
                            .reasonForReapplication("Existing ReapplicationReason")
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
                      "reasonForReapplication": "Updated ReapplicationReason",
                      "ecfFlag": false,
                      "clientDetails": {"niNumber": "AB123456C"},
                      "scopingQuestions": {"priorLegalAidReason": "new reason"}
                    }
                    """))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    clearCache();
    ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getReasonForReapplication()).isEqualTo("Updated ReapplicationReason");
    assertThat(updated.getEcfFlag()).isFalse();
    assertThat(updated.getClientDetails().getNiNumber()).isEqualTo("AB123456C");
    assertThat(updated.getScopingQuestions().get("priorLegalAidReason").asText())
        .isEqualTo("new reason");
    assertThat(updated.getScopingQuestions().get("unrelatedAnswer").asText())
        .isEqualTo("preserve me");
  }

  @Test
  void shouldPreserveEmptyReapplicationReasonWhenSupplied() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .reasonForReapplication("Existing ReapplicationReason")
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)))
            .getId();
    clearCache();

    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eTag\":0,\"reasonForReapplication\":\"\"}"))
        .andExpect(status().isNoContent());

    clearCache();
    assertThat(
            applicationRepository.findById(applicationId).orElseThrow().getReasonForReapplication())
        .isEmpty();
  }

  @Test
  void shouldLeaveScopingQuestionsUnchangedForEmptyObject() throws Exception {
    JsonNode existingScopingQuestions =
        objectMapper.readTree(
            "{\"unrelatedAnswer\":\"preserve me\",\"priorLegalAidReason\":\"existing\"}");
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .reasonForReapplication("Legacy ReapplicationReason")
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
                .content("{\"eTag\":0,\"scopingQuestions\":{}}"))
        .andExpect(status().isNoContent())
        .andExpect(header().exists("ETag"));

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getScopingQuestions()).isEqualTo(existingScopingQuestions);
    assertThat(updated.getReasonForReapplication()).isEqualTo("Legacy ReapplicationReason");
    assertThat(eventRepository.findAll()).hasSize(1);
    assertThat(eventRepository.findAll().getFirst().getPayload().get("scopingQuestions").isEmpty())
        .isTrue();
  }

  @Test
  void shouldClearWholeScopingQuestionsValueWhenExplicitlyNull() throws Exception {
    JsonNode existingScopingQuestions = objectMapper.readTree("{\"answer\":\"preserve\"}");
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .reasonForReapplication("Legacy ReapplicationReason")
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
                .content("{\"eTag\":0,\"scopingQuestions\":null}"))
        .andExpect(status().isNoContent());

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getScopingQuestions()).isNull();
    assertThat(updated.getReasonForReapplication()).isEqualTo("Legacy ReapplicationReason");
    assertThat(eventRepository.findAll().getFirst().getPayload().get("scopingQuestions").isNull())
        .isTrue();
  }

  @Test
  void shouldUpdateNestedReasonWithoutChangingReapplicationReason() throws Exception {
    JsonNode existingScopingQuestions =
        objectMapper.readTree(
            "{\"unrelatedAnswer\":\"preserve me\",\"priorLegalAidReason\":\"old reason\"}");
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .reasonForReapplication("Legacy ReapplicationReason")
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
                    "{\"eTag\":0,\"scopingQuestions\":{\"priorLegalAidReason\":\"new reason\"}}"))
        .andExpect(status().isNoContent());

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getReasonForReapplication()).isEqualTo("Legacy ReapplicationReason");
    assertThat(updated.getScopingQuestions().get("priorLegalAidReason").asText())
        .isEqualTo("new reason");
    assertThat(updated.getScopingQuestions().get("unrelatedAnswer").asText())
        .isEqualTo("preserve me");
  }

  @Test
  void shouldNotMutateApplicationOrRecordEventOnEtagConflict() throws Exception {
    JsonNode existingScopingQuestions = objectMapper.readTree("{\"answer\":\"original\"}");
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(
                                ClientDetailsEntityGenerator.createWithoutId(
                                    clientBuilder -> clientBuilder.noFixedAbode(true)))
                            .reasonForReapplication("Legacy ReapplicationReason")
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
                    {"eTag":99,"reasonForReapplication":"changed","ecfFlag":null,
                     "clientDetails":{"firstName":"Changed","niNumber":null},
                     "scopingQuestions":{"answer":"changed"}}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.reason").value("APPLICATION_VERSION_CONFLICT"));

    clearCache();
    final ApplicationEntity unchanged = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(unchanged.getEtag()).isZero();
    assertThat(unchanged.getReasonForReapplication()).isEqualTo("Legacy ReapplicationReason");
    assertThat(unchanged.getEcfFlag()).isTrue();
    assertThat(unchanged.getClientDetails().getFirstName()).isEqualTo("Joe");
    assertThat(unchanged.getClientDetails().getNiNumber()).isEqualTo("AB123456Q");
    assertThat(unchanged.getScopingQuestions()).isEqualTo(existingScopingQuestions);
    assertThat(eventRepository.findAll()).isEmpty();
  }

  @Test
  void shouldReturnVersionConflictReason_whenEtagDoesNotMatch() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .providerFirmCode(FIRM_CODE)
                            .providerOfficeCode(PROVIDER_OFFICE_CODE)
                            .etag(1L)))
            .getId();
    clearCache();

    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eTag\":0,\"laaReference\":\"changed\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.reason").value("APPLICATION_VERSION_CONFLICT"));

    clearCache();
    final ApplicationEntity unchanged = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(unchanged.getEtag()).isEqualTo(1);
    assertThat(unchanged.getLaaReference()).isNull();
    assertThat(eventRepository.findAll()).isEmpty();
  }

  @Test
  void shouldIgnoreExplicitNullForLegacyEditFields() throws Exception {
    final UUID determinationId = UUID.randomUUID();
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .ufn("123456/12")
                            .laaReference("LAA-123")
                            .meansAssessmentRequired(true)
                            .typeOfNonMeans(false)
                            .contribution("20")
                            .determinationId(determinationId)
                            .declaration(DeclarationEntityGenerator.createWithoutId(null))
                            .evidence(
                                EvidenceEntity.builder()
                                    .evidenceExemptionCode("EXEMPT_01")
                                    .createdBy("SYSTEM")
                                    .modifiedBy("SYSTEM")
                                    .build())
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
                    {"eTag":0,"ufn":null,"laaReference":null,
                     "meansAssessmentRequired":null,"typeOfNonMeans":null,
                     "contribution":null,"determinationId":null,
                     "declaration":null,"evidence":null}
                    """))
        .andExpect(status().isNoContent());

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getUfn()).isEqualTo("123456/12");
    assertThat(updated.getLaaReference()).isEqualTo("LAA-123");
    assertThat(updated.getMeansAssessmentRequired()).isTrue();
    assertThat(updated.getTypeOfNonMeans()).isFalse();
    assertThat(updated.getContribution()).isEqualTo("20");
    assertThat(updated.getDeterminationId()).isEqualTo(determinationId);
    assertThat(updated.getDeclaration()).isNotNull();
    assertThat(updated.getEvidence().getEvidenceExemptionCode()).isEqualTo("EXEMPT_01");
  }

  @Test
  void shouldPatchTenCharacterUfnAndDeclaration() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .ufn("123456/12")
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
                    {"eTag":0,"ufn":"123456/123","declaration":
                     {"declarationConfirmation":true,"dateSigned":"2025-01-01"}}
                    """))
        .andExpect(status().isNoContent())
        .andExpect(header().string("ETag", "\"1\""));

    clearCache();
    final ApplicationEntity updated = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(updated.getUfn()).isEqualTo("123456/123");
    assertThat(updated.getDeclaration().isDeclarationConfirmation()).isTrue();
    assertThat(updated.getDeclaration().getDateSigned().toString()).isEqualTo("2025-01-01");

    mockMvc
        .perform(get("/api/v0/applications/{id}", applicationId).withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ufn").value("123456/123"))
        .andExpect(jsonPath("$.declaration.declarationConfirmation").value(true));
  }

  @Test
  void shouldRejectElevenCharacterUfnWithoutChangingUfnOrDeclaration() throws Exception {
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .declaration(DeclarationEntityGenerator.createWithoutId(null))
                            .ufn("123456/12")
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
                    {"eTag":0,"ufn":"123456/1234","declaration":
                     {"declarationConfirmation":true,"dateSigned":"2025-01-01"}}
                    """))
        .andExpect(status().isBadRequest());

    clearCache();
    final ApplicationEntity unchanged = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(unchanged.getUfn()).isEqualTo("123456/12");
    assertThat(unchanged.getDeclaration().isDeclarationConfirmation()).isFalse();
    assertThat(unchanged.getDeclaration().getDateSigned()).isNull();
  }

  @Test
  void shouldRejectDuplicateUfnWithoutChangingUfnOrDeclaration() throws Exception {
    applicationRepository.saveAndFlush(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                    .ufn("123456/12")
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    final UUID applicationId =
        applicationRepository
            .saveAndFlush(
                ApplicationEntityGenerator.createWithoutId(
                    builder ->
                        builder
                            .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                            .declaration(DeclarationEntityGenerator.createWithoutId(null))
                            .ufn("654321/98")
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
                    {"eTag":0,"ufn":"123456/12","declaration":
                     {"declarationConfirmation":true,"dateSigned":"2025-01-01"}}
                    """))
        .andExpect(status().isConflict());

    clearCache();
    final ApplicationEntity unchanged = applicationRepository.findById(applicationId).orElseThrow();
    assertThat(unchanged.getUfn()).isEqualTo("654321/98");
    assertThat(unchanged.getDeclaration().isDeclarationConfirmation()).isFalse();
    assertThat(unchanged.getDeclaration().getDateSigned()).isNull();
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

  @Test
  void shouldReturnGone_whenApplicationIsDeleted() throws Exception {
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
                            .deleted(true)))
            .getId();
    clearCache();

    // Act + Assert
    mockMvc
        .perform(
            patch(TestConstants.EditApplication, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eTag\":0,\"laaReference\":\"changed\"}"))
        .andExpect(status().isGone());
  }
}
