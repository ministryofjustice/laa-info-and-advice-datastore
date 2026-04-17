package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.CaseDetailsEntity;

/** Repository for managing evidence entities. */
@Repository
public interface CaseDetailsRepository extends JpaRepository<CaseDetailsEntity, UUID> {}
