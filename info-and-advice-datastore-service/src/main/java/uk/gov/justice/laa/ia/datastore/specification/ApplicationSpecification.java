package uk.gov.justice.laa.ia.datastore.specification;

import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;

/** The specification for filtering ApplicationEntity at a database level. */
public class ApplicationSpecification {
  private ApplicationSpecification() {
    // private constructor to prevent instantiation
  }

  /** Setups a specification for filtering ApplicationEntity. */
  public static Specification<ApplicationEntity> filterBy(UUID officeId) {
    return hasOfficeId(officeId);
  }

  /** Returns a specification that filters ApplicationEntity by officeId. */
  protected static Specification<ApplicationEntity> hasOfficeId(UUID officeId) {
    if (officeId == null) {
      return Specification.unrestricted();
    }
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("providerOfficeId"), officeId);
  }
}
