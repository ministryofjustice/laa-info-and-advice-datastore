package uk.gov.justice.laa.ia.datastore.repository.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for filtering ApplicationEntity by status via ApplicationSpecification. */
@WithMockUser()
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ApplicationStateSpecificationIntegrationTest extends BaseIntegrationTest {
  @Test
  void whenStatusSpecificationIsUsed_thenReturnApplicationsWithMatchingStatus() {
    // Arrange
    final ApplicationEntity draftApplication =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.applicationState(ApplicationState.DRAFT);
            });
    final ApplicationEntity completedApplication =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.applicationState(ApplicationState.COMPLETED);
            });
    applicationRepository.saveAndFlush(draftApplication);
    applicationRepository.saveAndFlush(completedApplication);
    clearCache();

    // create specification
    Specification<ApplicationEntity> specification =
        ApplicationSpecification.filterBy(null, ApplicationState.COMPLETED);

    // Act
    List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).hasSize(1);
    assertThat(applications.iterator().next().getApplicationState())
        .isEqualTo(ApplicationState.COMPLETED);
  }

  @Test
  void whenStatusIsNull_thenReturnAllApplications() {
    // Arrange
    final ApplicationEntity draftApplication =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.applicationState(ApplicationState.DRAFT);
            });
    final ApplicationEntity completedApplication =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.applicationState(ApplicationState.COMPLETED);
            });
    applicationRepository.saveAndFlush(draftApplication);
    applicationRepository.saveAndFlush(completedApplication);
    clearCache();

    // create specification
    Specification<ApplicationEntity> specification = ApplicationSpecification.filterBy(null, null);

    // Act
    List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).hasSize(2);
  }
}
