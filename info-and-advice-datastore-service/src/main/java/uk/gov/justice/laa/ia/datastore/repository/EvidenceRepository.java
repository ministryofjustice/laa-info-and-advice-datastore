package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;

/** Repository for managing evidence entities. */
@Repository
public interface EvidenceRepository extends JpaRepository<EvidenceEntity, UUID> {}
