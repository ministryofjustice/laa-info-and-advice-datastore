package uk.gov.justice.laa.ia.datastore.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;

/** Repository for EventEntity. */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

  List<EventEntity> findByApplicationId(UUID applicationId);
}
