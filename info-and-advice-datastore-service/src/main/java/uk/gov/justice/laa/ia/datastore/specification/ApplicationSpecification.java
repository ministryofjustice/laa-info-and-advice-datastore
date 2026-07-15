package uk.gov.justice.laa.ia.datastore.specification;

import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;

/** The specification for filtering ApplicationEntity at a database level. */
public class ApplicationSpecification {
  private ApplicationSpecification() {
    // private constructor to prevent instantiation
  }

  private static final UUID EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  /** Setups a specification for filtering ApplicationEntity by applicationId and providerFirmId. */
  public static Specification<ApplicationEntity> findById(UUID applicationId, UUID providerFirmId) {
    return filterByProviderFirmId(providerFirmId)
        .and(
            (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), applicationId));
  }

  /** Setups a specification for filtering ApplicationEntity by providerFirmId. */
  public static Specification<ApplicationEntity> filterByProviderFirmId(UUID providerFirmId) {
    if (providerFirmId == null || providerFirmId.equals(EMPTY_UUID)) {
      throw new IllegalArgumentException("providerFirmId must not be null or empty UUID");
    }
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("providerFirmId"), providerFirmId);
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
