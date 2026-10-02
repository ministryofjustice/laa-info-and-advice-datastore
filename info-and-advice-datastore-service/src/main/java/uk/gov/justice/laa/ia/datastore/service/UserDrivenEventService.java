package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;

/**
 * Service for recording user-driven mutation events in the same transaction as the triggering
 * operation.
 */
@Service
@RequiredArgsConstructor
public class UserDrivenEventService {

  private static final UUID EMPTY_APPLICATION_ID = new UUID(0L, 0L);

  private final EventRepository repository;
  private final UserContext userContext;
  private final HttpServletRequest request;
  private final ObjectMapper objectMapper;

  /**
   * Records a mutation event. Must be called within an active transaction so that the event and the
   * mutation it describes are committed or rolled back together.
   *
   * @param payload the request body that caused the mutation.
   * @param providerOfficeCode the provider office code of the application the mutation relates to,
   *     as stored on the application record.
   * @param applicationId the ID of the application the mutation relates to, or an empty guid if not
   *     known.
   */
  public void record(Object payload, String providerOfficeCode, UUID applicationId) {
    EventEntity event =
        EventEntity.builder()
            .changedBy(userContext.getCurrentUser())
            .providerFirmCode(userContext.getProviderFirmCode())
            .providerOfficeCode(providerOfficeCode)
            .applicationId(applicationId != null ? applicationId : EMPTY_APPLICATION_ID)
            .correlationId(userContext.getCorrelationId())
            .serviceName(userContext.getServiceName())
            .httpMethod(request.getMethod())
            .urlPath(request.getRequestURI())
            .payload(objectMapper.valueToTree(payload))
            .build();
    repository.save(event);
  }
}
