package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;

/** Repository for managing Address entities. */
@Repository
public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {}
