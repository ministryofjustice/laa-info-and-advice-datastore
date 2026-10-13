package uk.gov.justice.laa.ia.datastore.repository.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

/** Integration tests for ApplicationSpecification.findByExpiredDataRetentionDateAndStatus. */
@WithMockUser()
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ExpiredDataRetentionDateSpecificationIntegrationTest extends BaseIntegrationTest {

  @Test
  void whenCompletedAndDataRetentionDateHasExpired_thenReturned() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .applicationState(ApplicationState.COMPLETED)
                    .dataRetentionDate(Instant.now().minus(1, ChronoUnit.DAYS)));
    applicationRepository.saveAndFlush(application);
    clearCache();

    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.findByExpiredDataRetentionDateAndStatus();

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).hasSize(1);
    assertThat(applications.getFirst().getId()).isEqualTo(application.getId());
  }

  @Test
  void whenCompletedAndDataRetentionDateIsInTheFuture_thenNotReturned() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .applicationState(ApplicationState.COMPLETED)
                    .dataRetentionDate(Instant.now().plus(1, ChronoUnit.DAYS)));
    applicationRepository.saveAndFlush(application);
    clearCache();

    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.findByExpiredDataRetentionDateAndStatus();

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).isEmpty();
  }

  @Test
  void whenCompletedAndDataRetentionDateIsNull_thenNotReturned() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .applicationState(ApplicationState.COMPLETED)
                    .dataRetentionDate(null));
    applicationRepository.saveAndFlush(application);
    clearCache();

    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.findByExpiredDataRetentionDateAndStatus();

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).isEmpty();
  }

  @Test
  void whenNotCompletedAndDataRetentionDateHasExpired_thenNotReturned() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .applicationState(ApplicationState.DRAFT)
                    .dataRetentionDate(Instant.now().minus(1, ChronoUnit.DAYS)));
    applicationRepository.saveAndFlush(application);
    clearCache();

    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.findByExpiredDataRetentionDateAndStatus();

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).isEmpty();
  }

  @Test
  void whenCompletedExpiredAndAlreadyDeleted_thenNotReturned() {
    // Arrange
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .applicationState(ApplicationState.COMPLETED)
                    .dataRetentionDate(Instant.now().minus(1, ChronoUnit.DAYS))
                    .deleted(true)
                    .deletedAt(Instant.now()));
    applicationRepository.saveAndFlush(application);
    clearCache();

    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.findByExpiredDataRetentionDateAndStatus();

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).isEmpty();
  }
}
