package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the EligibilityResultRepository. */
@WithMockUser()
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class EligibilityResultRepositoryIntegrationTest extends BaseIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSaveAndRetrieveEligibilityResult() {
    // Arrange: Create and save an application first (for the foreign key)
    ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
            });
    ApplicationEntity savedApplication = applicationRepository.saveAndFlush(application);
    // Arrange: Create eligibility result
    EligibilityResultEntity entity =
        EligibilityResultEntityGenerator.createEligibilityResult(
            savedApplication.getId(), "ELIGIBLE", 100);

    // Act
    EligibilityResultEntity savedEntity = eligibilityResultRepository.saveAndFlush(entity);
    clearCache();

    // Assert
    EligibilityResultEntity retrievedEntity =
        eligibilityResultRepository.findById(savedEntity.getEligibilityResultId()).orElseThrow();

    assertThat(retrievedEntity.getEligibilityResultId()).isNotNull();
    assertThat(retrievedEntity.getApplicationId()).isEqualTo(savedApplication.getId());
    assertThat(retrievedEntity.getData()).isEqualTo(entity.getData());
    assertThat(retrievedEntity.getResultJson()).isEqualTo(entity.getResultJson());
    assertThat(retrievedEntity.getCreatedAt()).isNotNull();
  }

  @Test
  void shouldRetrieveMultipleEligibilityResultsForApplication() {
    // Arrange: Create and save an application first (for the foreign key)
    ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
            });
    ApplicationEntity savedApplication = applicationRepository.saveAndFlush(application);

    // Arrange: Create multiple eligibility results
    EligibilityResultEntity entity1 =
        EligibilityResultEntityGenerator.createEligibilityResult(
            savedApplication.getId(), "ELIGIBLE", 100);
    EligibilityResultEntity entity2 =
        EligibilityResultEntityGenerator.createEligibilityResult(
            savedApplication.getId(), "INELIGIBLE", 50);

    eligibilityResultRepository.saveAndFlush(entity1);
    eligibilityResultRepository.saveAndFlush(entity2);
    clearCache();

    // Act
    final ApplicationEntity getApplication =
        applicationRepository.findById(savedApplication.getId()).orElseThrow();

    // Assert
    assertThat(getApplication.getEligibilityResults()).hasSize(2);
    assertSetContainsEligiblityResult(getApplication.getEligibilityResults(), entity1);
    assertSetContainsEligiblityResult(getApplication.getEligibilityResults(), entity2);
  }

  private static void assertSetContainsEligiblityResult(
      Iterable<EligibilityResultEntity> eligibilityResults, EligibilityResultEntity expected) {
    assertThat(eligibilityResults)
        .anySatisfy(
            result -> {
              assertThat(result.getEligibilityResultId())
                  .isEqualTo(expected.getEligibilityResultId());
              assertThat(result.getApplicationId()).isEqualTo(expected.getApplicationId());
              assertThat(result.getResultJson()).isEqualTo(expected.getResultJson());
              assertThat(result.getCreatedAt()).isNotNull();
            });
  }
}
