package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.repository.EventRepository;

/** Service for recording mutation events triggered by system processes such as scheduled tasks. */
@Service
@RequiredArgsConstructor
public class SystemDrivenEventService {

  private static final String SYSTEM_USER = "SYSTEM";
  private static final String HTTP_METHOD = "SCHEDULED";
  private static final UUID EMPTY_APPLICATION_ID = new UUID(0L, 0L);

  private final EventRepository repository;
  private final ObjectMapper objectMapper;

  /**
   * Records a mutation event raised by a system process. Must be called within an active
   * transaction so that the event and the mutation it describes are committed or rolled back
   * together.
   *
   * @param payload the data describing the mutation, e.g. a before/after diff.
   * @param providerOfficeCode the provider office code of the application the mutation relates to.
   * @param providerFirmCode the provider firm code of the application the mutation relates to.
   * @param source the name of the system process that triggered the event.
   * @param applicationId the ID of the application the mutation relates to, or an empty guid if not
   *     known.
   */
  public void record(
      Object payload,
      String providerOfficeCode,
      String providerFirmCode,
      String source,
      UUID applicationId) {
    EventEntity event =
        EventEntity.builder()
            .changedBy(SYSTEM_USER)
            .providerOfficeCode(providerOfficeCode)
            .providerFirmCode(providerFirmCode)
            .applicationId(applicationId != null ? applicationId : EMPTY_APPLICATION_ID)
            .serviceName(SYSTEM_USER)
            .httpMethod(HTTP_METHOD)
            .urlPath(source)
            .payload(objectMapper.valueToTree(payload))
            .build();
    repository.save(event);
  }
}
