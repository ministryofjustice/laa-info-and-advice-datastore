package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceGenerator;
import uk.gov.justice.laa.ia.datastore.generator.StartApplicationCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.EligibilityData;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateMeansDataCommand;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration tests verifying that an event is recorded for each mutating operation. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class EventsIntegrationTest extends BaseIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldRecordEvent_whenApplicationCreated() throws Exception {
    StartApplicationCommand command =
        StartApplicationCommandGenerator.create(
            builder -> builder.providerOfficeCode(PROVIDER_OFFICE_CODE));
    String payload = toJson(command);

    mockMvc
        .perform(
            post("/api/v0/applications:start-application")
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated());

    EventEntity event = assertSingleEventRecorded("POST", "/api/v0/applications:start-application");
    JsonNode originalClient = objectMapper.readTree(payload).get("client");
    JsonNode storedClient = event.getPayload().get("client");
    assertPiiRedacted(event, storedClient, originalClient, "firstName");
    assertPiiRedacted(event, storedClient, originalClient, "lastName");
    assertPiiRedacted(event, storedClient, originalClient, "dateOfBirth");
    assertPiiRedacted(event, storedClient, originalClient, "nationalInsuranceNumber");
    assertThat(storedClient.get("noFixedAbode")).isEqualTo(originalClient.get("noFixedAbode"));
    assertThat(event.getPayload().get("providerOfficeCode"))
        .isEqualTo(objectMapper.readTree(payload).get("providerOfficeCode"));
  }

  @Test
  void shouldRecordEvent_whenMeansDataUpdated() throws Exception {
    final UUID applicationId = savedApplicationId();
    final UpdateMeansDataCommand command =
        UpdateMeansDataCommand.builder()
            .eTag(0L)
            .data(EligibilityData.builder().clientAge("18-24").build())
            .result(new ObjectMapper().createObjectNode().put("status", "ELIGIBLE"))
            .build();
    final String payload = toJson(command);

    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());

    EventEntity event = assertSingleEventRecorded("PUT", applicationId.toString());
    assertThat(event.getPayload()).isEqualTo(objectMapper.readTree(payload));
  }

  @Test
  void shouldRecordEvent_whenDeclarationUpdated() throws Exception {
    final UUID applicationId = savedApplicationId();
    final String payload =
        toJson(
            DeclarationCommand.builder()
                .eTag(0L)
                .declarationConfirmation(true)
                .dateSigned(java.time.LocalDate.now())
                .build());

    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());

    EventEntity event = assertSingleEventRecorded("PATCH", applicationId.toString());
    assertThat(event.getPayload()).isEqualTo(objectMapper.readTree(payload));
  }

  @Test
  void shouldRecordEvent_whenEvidenceUpdated() throws Exception {
    final UUID applicationId = savedApplicationId();
    final String payload = toJson(EvidenceGenerator.createUpdateEvidenceCommand(0L));

    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());

    EventEntity event = assertSingleEventRecorded("PUT", applicationId.toString());
    assertThat(event.getPayload()).isEqualTo(objectMapper.readTree(payload));
  }

  @Test
  void shouldRecordEventsInOrder_whenMultipleMutationsOccur() throws Exception {
    UUID applicationId = savedApplicationId();
    clearCache();

    mockMvc
        .perform(
            patch(TestConstants.UpdateDeclarationData, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(
                        DeclarationCommand.builder()
                            .eTag(0L)
                            .declarationConfirmation(true)
                            .dateSigned(java.time.LocalDate.now())
                            .build())))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(EvidenceGenerator.createUpdateEvidenceCommand(1L))))
        .andExpect(status().isNoContent());

    clearCache();
    List<EventEntity> events =
        eventRepository.findAll().stream()
            .sorted((a, b) -> a.getSequenceNumber().compareTo(b.getSequenceNumber()))
            .toList();
    assertThat(events).hasSize(2);
    assertThat(events.get(0).getSequenceNumber()).isLessThan(events.get(1).getSequenceNumber());
    assertThat(events.get(0).getUrlPath()).contains(":update-declaration-data");
    assertThat(events.get(1).getUrlPath()).contains(":update-evidence");
  }

  private EventEntity assertSingleEventRecorded(String expectedMethod, String expectedUrlContains)
      throws Exception {
    clearCache();
    List<EventEntity> events = eventRepository.findAll();
    assertThat(events).hasSize(1);
    EventEntity event = events.getFirst();
    assertThat(event.getHttpMethod()).isEqualTo(expectedMethod);
    assertThat(event.getUrlPath()).contains(expectedUrlContains);
    assertThat(event.getChangedBy()).isEqualTo("SYSTEM");
    assertThat(event.getProviderFirmCode()).isEqualTo(FIRM_CODE);
    assertThat(event.getSequenceNumber()).isNotNull();
    assertThat(event.getCreatedAt()).isNotNull();
    return event;
  }

  // Asserts a PII field was replaced by a UUID in the stored payload, resolvable via piiData.
  private static void assertPiiRedacted(
      EventEntity event, JsonNode storedParent, JsonNode originalParent, String field) {
    String uuid = storedParent.get(field).asText();
    assertThat(uuid).isNotEqualTo(originalParent.get(field).asText());
    assertThat(event.getPiiData().get(uuid)).isEqualTo(originalParent.get(field));
  }

  private UUID savedApplicationId() {
    return applicationRepository
        .saveAndFlush(
            ApplicationEntityGenerator.createWithoutId(
                builder ->
                    builder
                        .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                        .providerFirmCode(FIRM_CODE)
                        .providerOfficeCode(PROVIDER_OFFICE_CODE)))
        .getId();
  }
}
