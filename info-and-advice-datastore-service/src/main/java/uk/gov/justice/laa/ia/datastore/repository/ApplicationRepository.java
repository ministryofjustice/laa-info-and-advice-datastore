package uk.gov.justice.laa.ia.datastore.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;

/** Repository for managing application entities. */
@Repository
public interface ApplicationRepository
    extends JpaRepository<ApplicationEntity, UUID>, JpaSpecificationExecutor<ApplicationEntity> {

  @Override
  @EntityGraph(
      attributePaths = {
        "clientDetails",
        "clientDetails.address",
        "declaration",
        "eligibilityResults",
        "evidence"
      })
  Optional<ApplicationEntity> findOne(Specification<ApplicationEntity> spec);

  @Override
  @EntityGraph(
      attributePaths = {
        "clientDetails",
        "clientDetails.address",
        "declaration",
        "eligibilityResults",
        "evidence"
      })
  Page<ApplicationEntity> findAll(Specification<ApplicationEntity> spec, Pageable pageable);
}
