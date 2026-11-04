package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityAssessmentEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the EligibilityAssessmentRepository. */
@WithMockUser()
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class EligibilityAssessmentRepositoryIntegrationTest extends BaseIntegrationTest {

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
    EligibilityAssessmentEntity entity =
        EligibilityAssessmentEntityGenerator.createEligibilityAssessment(savedApplication.getId());

    // Act
    EligibilityAssessmentEntity savedEntity = eligibilityAssessmentRepository.saveAndFlush(entity);
    clearCache();

    // Assert
    EligibilityAssessmentEntity retrievedEntity =
        eligibilityAssessmentRepository
            .findById(savedEntity.getEligibilityAssessmentId())
            .orElseThrow();

    assertThat(retrievedEntity.getEligibilityAssessmentId()).isNotNull();
    assertThat(retrievedEntity.getApplicationId()).isEqualTo(savedApplication.getId());
    assertThat(retrievedEntity.getData()).isEqualTo(entity.getData());
    assertThat(retrievedEntity.getResultJson()).isEqualTo(entity.getResultJson());
    assertThat(retrievedEntity.getCreatedAt()).isNotNull();
  }

  @Test
  void shouldRetrieveMultipleEligibilityAssessmentsForApplication() {
    // Arrange: Create and save an application first (for the foreign key)
    ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
            });
    ApplicationEntity savedApplication = applicationRepository.saveAndFlush(application);

    // Arrange: Create multiple eligibility results
    EligibilityAssessmentEntity entity1 =
        EligibilityAssessmentEntityGenerator.createEligibilityAssessment(savedApplication.getId());
    EligibilityAssessmentEntity entity2 =
        EligibilityAssessmentEntityGenerator.createEligibilityAssessment(savedApplication.getId());

    eligibilityAssessmentRepository.saveAndFlush(entity1);
    eligibilityAssessmentRepository.saveAndFlush(entity2);
    clearCache();

    // Act
    final ApplicationEntity getApplication =
        applicationRepository.findById(savedApplication.getId()).orElseThrow();

    // Assert
    assertThat(getApplication.getEligibilityAssessments()).hasSize(2);
    assertSetContainsEligiblityResult(getApplication.getEligibilityAssessments(), entity1);
    assertSetContainsEligiblityResult(getApplication.getEligibilityAssessments(), entity2);
  }

  private static void assertSetContainsEligiblityResult(
      Iterable<EligibilityAssessmentEntity> eligibilityAssessments,
      EligibilityAssessmentEntity expected) {
    assertThat(eligibilityAssessments)
        .anySatisfy(
            result -> {
              assertThat(result.getEligibilityAssessmentId())
                  .isEqualTo(expected.getEligibilityAssessmentId());
              assertThat(result.getApplicationId()).isEqualTo(expected.getApplicationId());
              assertThat(result.getResultJson()).isEqualTo(expected.getResultJson());
              assertThat(result.getCreatedAt()).isNotNull();
            });
  }
}
