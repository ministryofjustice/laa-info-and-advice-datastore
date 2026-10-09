package uk.gov.justice.laa.ia.datastore.specification;

import jakarta.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.EligibilityIndication;

/** The specification for filtering ApplicationEntity at a database level. */
public class ApplicationSpecification {
  private ApplicationSpecification() {
    // private constructor to prevent instantiation
  }

  /** Setups a specification for finding completed applications without a data retention date. */
  public static Specification<ApplicationEntity> findByMissingDataRetentionDateAndStatus() {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.and(
            criteriaBuilder.isNull(root.get("dataRetentionDate")),
            criteriaBuilder.equal(root.get("applicationState"), ApplicationState.COMPLETED));
  }

  /**
   * Setups a specification for finding completed, not-yet-deleted applications whose data retention
   * date has expired.
   */
  public static Specification<ApplicationEntity> findByExpiredDataRetentionDateAndStatus() {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.and(
            criteriaBuilder.isNotNull(root.get("dataRetentionDate")),
            criteriaBuilder.lessThanOrEqualTo(root.get("dataRetentionDate"), Instant.now()),
            criteriaBuilder.equal(root.get("applicationState"), ApplicationState.COMPLETED),
            criteriaBuilder.isFalse(root.get("deleted")));
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

  /**
   * Setups a specification for the default constraints applied to every application listing: must
   * match the providerFirmCode, must have an officeCode in the user's authorized officeCodes, and
   * must not have been deleted.
   */
  public static Specification<ApplicationEntity> filterByDefaultConstraints(
      String providerFirmCode, List<String> officeCodes) {
    return filterByProviderFirmCode(providerFirmCode)
        .and(filterByProviderOfficesCodes(officeCodes))
        .and(notExpired());
  }

  /**
   * Setups a specification for filtering out ApplicationEntity that have expired data retention
   * dates.
   */
  public static Specification<ApplicationEntity> notExpired() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("deleted"));
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
      String officeId, ApplicationState status, EligibilityIndication eligibilityIndication) {
    return hasOfficeId(officeId)
        .and(hasStatus(status))
        .and(hasEligibilityIndication(eligibilityIndication));
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
      EligibilityIndication eligibilityIndication) {
    if (eligibilityIndication == null) {
      return Specification.unrestricted();
    }
    final Boolean indication = eligibilityIndication == EligibilityIndication.ELIGIBLE;
    return (root, query, criteriaBuilder) -> {
      var eligibilityJoin = root.join("eligibilityResults", JoinType.LEFT);
      query.distinct(true);
      return criteriaBuilder.equal(eligibilityJoin.get("indication"), indication);
    };
  }
}
