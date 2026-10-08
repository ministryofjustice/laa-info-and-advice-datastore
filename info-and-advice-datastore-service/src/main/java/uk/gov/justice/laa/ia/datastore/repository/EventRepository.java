package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;

/** Repository for EventEntity. */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

  /**
   * Checks whether an event with the given payload hash has already been recorded for the given
   * application and provider office code, used to detect duplicate request submissions.
   *
   * @param applicationId the application ID, or the empty guid if not yet known
   * @param providerOfficeCode the provider office code the request relates to
   * @param payloadHash the hash of the request payload
   * @return true if a matching event already exists
   */
  boolean existsByApplicationIdAndProviderOfficeCodeAndPayloadHash(
      UUID applicationId, String providerOfficeCode, String payloadHash);
}
