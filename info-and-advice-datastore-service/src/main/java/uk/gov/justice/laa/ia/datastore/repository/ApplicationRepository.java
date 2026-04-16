package uk.gov.justice.laa.ia.datastore.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;

/** Repository for managing application entities. */
@Repository
public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {}
