package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the {@link ApplicationEntity}. */
@ExtendWith(MockitoExtension.class)
public class ApplicationEntityTests {
  @Test
  void
      givenApplicationWithEligibilityResults_thenGetMostRecentEligibilityResultReturnsMostRecent() {
    // Arrange
    final ApplicationEntity applicationEntity = new ApplicationEntity();
    final EligibilityResultEntity olderEligibilityResult =
        EligibilityResultEntity.builder()
            .createdAt(java.time.Instant.now().minusSeconds(3600))
            .build();
    final EligibilityResultEntity newerEligibilityResult =
        EligibilityResultEntity.builder().createdAt(java.time.Instant.now()).build();
    applicationEntity.setEligibilityResults(
        java.util.Set.of(olderEligibilityResult, newerEligibilityResult));

    // Act
    final EligibilityResultEntity mostRecentEligibilityResult =
        applicationEntity.getMostRecentEligibilityResult();

    // Assert
    assertThat(mostRecentEligibilityResult).isEqualTo(newerEligibilityResult);
  }

  @Test
  void givenApplicationWithNullEligibilityResults_thenGetMostRecentEligibilityResultReturnsNull() {
    // Arrange
    final ApplicationEntity applicationEntity = new ApplicationEntity();
    // Act
    final EligibilityResultEntity mostRecentEligibilityResult =
        applicationEntity.getMostRecentEligibilityResult();

    // Assert
    assertThat(mostRecentEligibilityResult).isNull();
  }

  @Test
  void givenApplicationWithEmptyEligibilityResults_thenGetMostRecentEligibilityResultReturnsNull() {
    // Arrange
    final ApplicationEntity applicationEntity = new ApplicationEntity();
    applicationEntity.setEligibilityResults(java.util.Collections.emptySet());
    // Act
    final EligibilityResultEntity mostRecentEligibilityResult =
        applicationEntity.getMostRecentEligibilityResult();

    // Assert
    assertThat(mostRecentEligibilityResult).isNull();
  }
}
