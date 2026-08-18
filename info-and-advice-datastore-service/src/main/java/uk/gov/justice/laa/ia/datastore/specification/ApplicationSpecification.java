package uk.gov.justice.laa.ia.datastore.specification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;

/** The specification for filtering ApplicationEntity at a database level. */
public class ApplicationSpecification {
  private ApplicationSpecification() {
    // private constructor to prevent instantiation
  }

  /**
   * Setups a specification for filtering ApplicationEntity by applicationId and providerFirmCode.
   */
  public static Specification<ApplicationEntity> findById(
      UUID applicationId, String providerFirmCode) {
    return filterByProviderFirmCode(providerFirmCode)
        .and(
            (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), applicationId));
  }

  /** Setups a specification for filtering ApplicationEntity by providerFirmCode and officeCodes. */
  public static Specification<ApplicationEntity> filterByProviderContractInformation(
      String providerFirmCode, List<String> officeCodes) {
    return filterByProviderFirmCode(providerFirmCode)
        .and(filterByProviderOfficesCodes(officeCodes));
  }

  /** Setups a specification for filtering ApplicationEntity by providerFirmCode. */
  public static Specification<ApplicationEntity> filterByProviderFirmCode(String providerFirmCode) {
    if (providerFirmCode == null || providerFirmCode.isBlank()) {
      throw new IllegalArgumentException("providerFirmCode must not be null or blank");
    }
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("providerFirmCode"), providerFirmCode);
  }

  /**
   * Setups a specification for filtering ApplicationEntity by matching it's officeCode to the list
   * of {@code officeCodes}.
   */
  public static Specification<ApplicationEntity> filterByProviderOfficesCodes(
      List<String> officeCodes) {
    if (officeCodes == null || officeCodes.isEmpty()) {
      throw new IllegalArgumentException("officeCodes must not be null or empty");
    }
    return (root, query, criteriaBuilder) -> root.get("providerOfficeCode").in(officeCodes);
  }

  /** Setups a specification for filtering ApplicationEntity. */
  public static Specification<ApplicationEntity> filterBy(
      String officeId, ApplicationState status) {
    return hasOfficeId(officeId).and(hasStatus(status));
  }

  /** Returns a specification that filters ApplicationEntity by status. */
  protected static Specification<ApplicationEntity> hasStatus(ApplicationState status) {
    if (status == null) {
      return Specification.unrestricted();
    }
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("applicationState"), status);
  }

  /** Returns a specification that filters ApplicationEntity by officeId. */
  protected static Specification<ApplicationEntity> hasOfficeId(String officeId) {
    if (officeId == null || officeId.isBlank()) {
      return Specification.unrestricted();
    }
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get("providerOfficeCode"), officeId);
  }

  /** Returns a specification that filters ApplicationEntity by eligibility indication. */
  public static Specification<ApplicationEntity> hasEligibilityIndication(
      uk.gov.justice.laa.ia.datastore.model.EligibilityIndication eligibilityIndication) {
    if (eligibilityIndication == null) {
      return Specification.unrestricted();
    }
    final Boolean indication =
        eligibilityIndication
            == uk.gov.justice.laa.ia.datastore.model.EligibilityIndication.ELIGIBLE;
    return (root, query, criteriaBuilder) -> {
      var eligibilityJoin =
          root.join("eligibilityResults", jakarta.persistence.criteria.JoinType.LEFT);
      query.distinct(true);
      return criteriaBuilder.equal(eligibilityJoin.get("indication"), indication);
    };
  }
}
