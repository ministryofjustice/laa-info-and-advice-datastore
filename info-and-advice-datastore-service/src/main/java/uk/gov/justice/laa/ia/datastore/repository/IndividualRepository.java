package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;

/** Repository for managing item entities. */
@Repository
public interface IndividualRepository extends JpaRepository<IndividualEntity, UUID> {}
