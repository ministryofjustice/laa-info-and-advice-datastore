package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;

/** Unit tests for {@link SystemDrivenEventService}. */
@ExtendWith(MockitoExtension.class)
class SystemDrivenEventServiceTest {

  @Mock private EventRepository repository;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private SystemDrivenEventService sut;

  @Test
  void shouldSaveEventWithCorrectFields() {
    // Arrange
    final Object payload = new Object();
    final JsonNode payloadNode = new ObjectMapper().createObjectNode();
    when(objectMapper.valueToTree(payload)).thenReturn(payloadNode);
    when(repository.save(any(EventEntity.class))).thenAnswer(i -> i.getArgument(0));
    final UUID applicationId = UUID.randomUUID();

    // Act
    sut.record(payload, "office-code-1", "firm-code-1", "DataRetentionDateResolver", applicationId);

    // Assert
    ArgumentCaptor<EventEntity> captor = ArgumentCaptor.forClass(EventEntity.class);
    verify(repository).save(captor.capture());
    EventEntity saved = captor.getValue();
    assertThat(saved.getChangedBy()).isEqualTo("SYSTEM");
    assertThat(saved.getProviderOfficeCode()).isEqualTo("office-code-1");
    assertThat(saved.getProviderFirmCode()).isEqualTo("firm-code-1");
    assertThat(saved.getApplicationId()).isEqualTo(applicationId);
    assertThat(saved.getServiceName()).isEqualTo("SYSTEM");
    assertThat(saved.getHttpMethod()).isEqualTo("SCHEDULED");
    assertThat(saved.getUrlPath()).isEqualTo("DataRetentionDateResolver");
    assertThat(saved.getPayload()).isEqualTo(payloadNode);
  }

  @Test
  void shouldFallBackToEmptyGuid_whenApplicationIdIsNull() {
    // Arrange
    final Object payload = new Object();
    final JsonNode payloadNode = new ObjectMapper().createObjectNode();
    when(objectMapper.valueToTree(payload)).thenReturn(payloadNode);
    when(repository.save(any(EventEntity.class))).thenAnswer(i -> i.getArgument(0));

    // Act
    sut.record(payload, "office-code-1", "firm-code-1", "DataRetentionDateResolver", null);

    // Assert
    ArgumentCaptor<EventEntity> captor = ArgumentCaptor.forClass(EventEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getApplicationId()).isEqualTo(new UUID(0L, 0L));
  }
}
