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

  /**
   * Checks whether another application (excluding the one identified by {@code id}) already has the
   * given UFN for the given provider office code.
   *
   * @param providerOfficeCode the provider office code
   * @param ufn the UFN to check for uniqueness
   * @param id the id of the application being updated, excluded from the check
   * @return true if a different application with the same office code and UFN exists
   */
  boolean existsByProviderOfficeCodeAndUfnAndIdNot(String providerOfficeCode, String ufn, UUID id);
}
