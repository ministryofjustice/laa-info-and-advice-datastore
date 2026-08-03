package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;

/** Service for recording mutation events in the same transaction as the triggering operation. */
@Service
@RequiredArgsConstructor
public class EventService {

  private final EventRepository repository;
  private final UserContext userContext;
  private final HttpServletRequest request;
  private final ObjectMapper objectMapper;

  /**
   * Records a mutation event. Must be called within an active transaction so that the event and the
   * mutation it describes are committed or rolled back together.
   *
   * @param payload the request body that caused the mutation.
   */
  public void record(Object payload) {
    EventEntity event =
        EventEntity.builder()
            .changedBy(userContext.getCurrentUser())
            .providerFirmCode(userContext.getProviderFirmCode())
            .providerOfficeId(userContext.getProviderOfficeId())
            .httpMethod(request.getMethod())
            .urlPath(request.getRequestURI())
            .payload(objectMapper.valueToTree(payload))
            .build();
    repository.save(event);
  }
}
