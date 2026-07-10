package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceGenerator;
import uk.gov.justice.laa.ia.datastore.generator.StartCaseCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration tests verifying that an event is recorded for each mutating operation. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class EventsIntegrationTest extends BaseIntegrationTest {

  @Test
  void shouldRecordEvent_whenApplicationCreated() throws Exception {
    // Arrange
    StartCaseCommand command = StartCaseCommandGenerator.create(null);

    // Act
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v0/applications:start-case")
                    .withBearerWriteToken()
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(command)))
            .andExpect(status().isCreated())
            .andReturn();

    // Assert
    clearCache();
    List<EventEntity> events = eventRepository.findAll();
    assertThat(events).hasSize(1);
    EventEntity event = events.getFirst();
    assertThat(event.getHttpMethod()).isEqualTo("POST");
    assertThat(event.getUrlPath()).isEqualTo("/api/v0/applications:start-case");
    assertThat(event.getChangedBy()).isEqualTo("SYSTEM");
    assertThat(event.getPayload()).isNotNull();
    assertThat(event.getSequenceNumber()).isNotNull();
    assertThat(event.getCreatedAt()).isNotNull();
  }

  @Test
  void shouldRecordEvent_whenMeansDataUpdated() throws Exception {
    // Arrange
    final UUID applicationId = savedApplicationId();
    final String payload =
        """
        {"determinationId": "%s", "meansAssessmentRequired": true}
        """
            .formatted(UUID.randomUUID());

    // Act
    mockMvc
        .perform(
            put("/api/v0/applications/{id}:update-means-data", applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk());

    // Assert
    clearCache();
    List<EventEntity> events = eventRepository.findAll();
    assertThat(events).hasSize(1);
    EventEntity event = events.getFirst();
    assertThat(event.getHttpMethod()).isEqualTo("PUT");
    assertThat(event.getUrlPath()).contains(applicationId.toString());
    assertThat(event.getChangedBy()).isEqualTo("SYSTEM");
    assertThat(event.getPayload()).isNotNull();
  }

  @Test
  void shouldRecordEvent_whenDeclarationUpdated() throws Exception {
    // Arrange
    final UUID applicationId = savedApplicationId();
    final String payload =
        toJson(DeclarationCommand.builder().declarationConfirmation(true).build());

    // Act
    mockMvc
        .perform(
            put(TestConstants.UpdateDeclaration, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());

    // Assert
    clearCache();
    List<EventEntity> events = eventRepository.findAll();
    assertThat(events).hasSize(1);
    EventEntity event = events.getFirst();
    assertThat(event.getHttpMethod()).isEqualTo("PUT");
    assertThat(event.getUrlPath()).contains(applicationId.toString());
    assertThat(event.getChangedBy()).isEqualTo("SYSTEM");
    assertThat(event.getPayload()).isNotNull();
  }

  @Test
  void shouldRecordEvent_whenEvidenceUpdated() throws Exception {
    // Arrange
    final UUID applicationId = savedApplicationId();
    final String payload = toJson(EvidenceGenerator.createEvidenceMap());

    // Act
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isNoContent());

    // Assert
    clearCache();
    List<EventEntity> events = eventRepository.findAll();
    assertThat(events).hasSize(1);
    EventEntity event = events.getFirst();
    assertThat(event.getHttpMethod()).isEqualTo("PUT");
    assertThat(event.getUrlPath()).contains(applicationId.toString());
    assertThat(event.getChangedBy()).isEqualTo("SYSTEM");
    assertThat(event.getPayload()).isNotNull();
  }

  @Test
  void shouldRecordEventsInOrder_whenMultipleMutationsOccur() throws Exception {
    // Arrange — use a flushed entity so the second request can find it
    UUID applicationId = savedApplicationId();
    clearCache();

    // Act — two mutations in sequence
    mockMvc
        .perform(
            put(TestConstants.UpdateDeclaration, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    toJson(DeclarationCommand.builder().declarationConfirmation(true).build())))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(EvidenceGenerator.createEvidenceMap())))
        .andExpect(status().isNoContent());

    // Assert — two events with strictly ascending sequence numbers
    clearCache();
    List<EventEntity> events =
        eventRepository.findAll().stream()
            .sorted((a, b) -> a.getSequenceNumber().compareTo(b.getSequenceNumber()))
            .toList();
    assertThat(events).hasSize(2);
    assertThat(events.get(0).getSequenceNumber()).isLessThan(events.get(1).getSequenceNumber());
    assertThat(events.get(0).getUrlPath()).contains("/declaration");
    assertThat(events.get(1).getUrlPath()).contains(":update-evidence");
  }

  private UUID savedApplicationId() {
    return applicationRepository
        .saveAndFlush(
            ApplicationEntityGenerator.createWithoutId(
                builder ->
                    builder.clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))))
        .getId();
  }
}
