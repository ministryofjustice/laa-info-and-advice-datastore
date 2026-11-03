package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the {@link ApplicationEntity}. */
@ExtendWith(MockitoExtension.class)
public class ApplicationEntityTests {
  @Test
  void givenEligibilityAssessments_thenGetMostRecentEligibilityAssessmentReturnsMostRecent() {
    // Arrange
    final ApplicationEntity applicationEntity = new ApplicationEntity();
    final EligibilityAssessmentEntity olderEligibilityResult =
        EligibilityAssessmentEntity.builder()
            .createdAt(java.time.Instant.now().minusSeconds(3600))
            .build();
    final EligibilityAssessmentEntity newerEligibilityResult =
        EligibilityAssessmentEntity.builder().createdAt(java.time.Instant.now()).build();
    applicationEntity.setEligibilityAssessments(
        java.util.Set.of(olderEligibilityResult, newerEligibilityResult));

    // Act
    final EligibilityAssessmentEntity mostRecentEligibilityAssessment =
        applicationEntity.getMostRecentEligibilityAssessment();

    // Assert
    assertThat(mostRecentEligibilityAssessment).isEqualTo(newerEligibilityResult);
  }

  @Test
  void givenNullEligibilityAssessments_thenGetMostRecentEligibilityAssessmentReturnsNull() {
    // Arrange
    final ApplicationEntity applicationEntity = new ApplicationEntity();
    // Act
    final EligibilityAssessmentEntity mostRecentEligibilityAssessment =
        applicationEntity.getMostRecentEligibilityAssessment();

    // Assert
    assertThat(mostRecentEligibilityAssessment).isNull();
  }

  @Test
  void givenEmptyEligibilityAssessments_thenGetMostRecentEligibilityAssessmentReturnsNull() {
    // Arrange
    final ApplicationEntity applicationEntity = new ApplicationEntity();
    applicationEntity.setEligibilityAssessments(java.util.Collections.emptySet());
    // Act
    final EligibilityAssessmentEntity mostRecentEligibilityAssessment =
        applicationEntity.getMostRecentEligibilityAssessment();

    // Assert
    assertThat(mostRecentEligibilityAssessment).isNull();
  }
}
