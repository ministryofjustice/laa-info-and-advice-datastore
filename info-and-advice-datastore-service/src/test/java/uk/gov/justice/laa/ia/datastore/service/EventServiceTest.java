package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;

/** Unit tests for {@link EventService}. */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

  @Mock private EventRepository repository;
  @Mock private UserContext userContext;
  @Mock private HttpServletRequest request;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private EventService sut;

  @Test
  void shouldSaveEventWithCorrectFields() {
    // Arrange
    final Object payload = new Object();
    final JsonNode payloadNode = new ObjectMapper().createObjectNode();
    when(userContext.getCurrentUser()).thenReturn("test-user");
    when(userContext.getProviderFirmCode()).thenReturn("123456");
    when(userContext.getProviderOfficeCode()).thenReturn("00000000-0000-0000-0000-000000000001");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRequestURI()).thenReturn("/api/v0/applications:start-application");
    when(objectMapper.valueToTree(payload)).thenReturn(payloadNode);
    when(repository.save(any(EventEntity.class))).thenAnswer(i -> i.getArgument(0));

    // Act
    sut.record(payload);

    // Assert
    ArgumentCaptor<EventEntity> captor = ArgumentCaptor.forClass(EventEntity.class);
    verify(repository).save(captor.capture());
    EventEntity saved = captor.getValue();
    assertThat(saved.getChangedBy()).isEqualTo("test-user");
    assertThat(saved.getProviderFirmCode()).isEqualTo("123456");
    assertThat(saved.getProviderOfficeCode()).isEqualTo("00000000-0000-0000-0000-000000000001");
    assertThat(saved.getHttpMethod()).isEqualTo("POST");
    assertThat(saved.getUrlPath()).isEqualTo("/api/v0/applications:start-application");
    assertThat(saved.getPayload()).isEqualTo(payloadNode);
  }
}
