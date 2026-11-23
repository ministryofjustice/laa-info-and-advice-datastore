package uk.gov.justice.laa.ia.datastore.specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.EligibilityIndication;

/**
 * Unit tests for how {@link ApplicationSpecification#filterBy} combines the eligibility indication
 * with the means-exempt filter. Database behaviour is covered by GetApplicationsIntegrationTest.
 */
@SuppressWarnings("unchecked")
class EligibilitySpecificationTests {

  private final Root<ApplicationEntity> root = mock(Root.class);
  private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
  private final CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
  private final Predicate indicationPredicate = mock(Predicate.class);
  private final Predicate meansExemptPredicate = mock(Predicate.class);
  private final Predicate combinedPredicate = mock(Predicate.class);
  private final Path<Boolean> meansAssessmentRequired = mock(Path.class);

  @BeforeEach
  void setUp() {
    Join<Object, Object> eligibilityJoin = mock(Join.class);
    Path<Object> indication = mock(Path.class);
    when(root.join("eligibilityAssessments", JoinType.LEFT)).thenReturn(eligibilityJoin);
    when(eligibilityJoin.get("indication")).thenReturn(indication);
    when(criteriaBuilder.equal(indication, true)).thenReturn(indicationPredicate);
    when(root.<Boolean>get("meansAssessmentRequired")).thenReturn(meansAssessmentRequired);
    when(criteriaBuilder.isFalse(meansAssessmentRequired)).thenReturn(meansExemptPredicate);
    when(criteriaBuilder.or(indicationPredicate, meansExemptPredicate))
        .thenReturn(combinedPredicate);
  }

  @Test
  void givenIncludeMeansExempt_whenFilterByIndication_thenMatchesIndicationOrMeansExempt() {
    Predicate predicate =
        ApplicationSpecification.filterBy(null, null, EligibilityIndication.ELIGIBLE, true)
            .toPredicate(root, query, criteriaBuilder);

    assertThat(predicate).isSameAs(combinedPredicate);
    verify(query).distinct(true);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = false)
  void givenMeansExemptNotIncluded_whenFilterByIndication_thenMatchesIndicationOnly(
      Boolean includeMeansExemptApplications) {
    Predicate predicate =
        ApplicationSpecification.filterBy(
                null, null, EligibilityIndication.ELIGIBLE, includeMeansExemptApplications)
            .toPredicate(root, query, criteriaBuilder);

    assertThat(predicate).isSameAs(indicationPredicate);
    verify(criteriaBuilder, never()).isFalse(any());
    verify(criteriaBuilder, never()).or(any(Predicate.class), eq(meansExemptPredicate));
  }

  @Test
  void givenNoIndication_whenFilterByWithIncludeMeansExempt_thenFlagIsIgnored() {
    Root<ApplicationEntity> unusedRoot = mock(Root.class);
    CriteriaBuilder unusedCriteriaBuilder = mock(CriteriaBuilder.class);

    Predicate predicate =
        ApplicationSpecification.filterBy(null, null, null, true)
            .toPredicate(unusedRoot, query, unusedCriteriaBuilder);

    assertThat(predicate).isNull();
    verifyNoInteractions(unusedRoot, unusedCriteriaBuilder);
  }
}
