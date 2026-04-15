package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;

/** Repository for managing declaration entities. */
@Repository
public interface DeclarationRepository extends JpaRepository<DeclarationEntity, UUID> {}
