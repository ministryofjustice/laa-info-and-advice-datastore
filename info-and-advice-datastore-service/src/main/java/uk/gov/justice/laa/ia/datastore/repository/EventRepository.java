package uk.gov.justice.laa.ia.datastore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;

/** Repository for EventEntity. */
@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {}
