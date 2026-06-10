package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;

/** Repository for managing client details entities. */
@Repository
public interface ClientDetailsRepository extends JpaRepository<ClientDetailsEntity, UUID> {}
