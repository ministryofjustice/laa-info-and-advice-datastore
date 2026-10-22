package uk.gov.justice.laa.ia.datastore.repository.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for ApplicationSpecification.findByExpiredDraft. */
@WithMockUser()
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ExpiredDraftSpecificationIntegrationTest extends BaseIntegrationTest {

  private static final Instant CUTOFF = Instant.now().minus(2 * 365, ChronoUnit.DAYS);

  @BeforeEach
  void disableModifiedAtTrigger() {
    // The trigger resets modified_at on every update. Postgres DDL is transactional, so this is
    // rolled back with the test transaction.
    entityManager
        .createNativeQuery(
            "ALTER TABLE applications DISABLE TRIGGER application_modified_at_trigger")
        .executeUpdate();
  }

  @Test
  void whenDraftModifiedBeforeCutoff_thenReturned() {
    // Arrange
    final ApplicationEntity application =
        saveApplication(ApplicationState.DRAFT, false, CUTOFF.minus(1, ChronoUnit.DAYS));

    // Act
    final List<ApplicationEntity> applications = findExpiredDrafts();

    // Assert
    assertThat(applications)
        .extracting(ApplicationEntity::getId)
        .containsExactly(application.getId());
  }

  @Test
  void whenDraftModifiedAfterCutoff_thenNotReturned() {
    // Arrange
    saveApplication(ApplicationState.DRAFT, false, CUTOFF.plus(1, ChronoUnit.DAYS));

    // Act
    final List<ApplicationEntity> applications = findExpiredDrafts();

    // Assert
    assertThat(applications).isEmpty();
  }

  @Test
  void whenDraftCreatedLongAgoButRecentlyModified_thenNotReturned() {
    // Arrange
    final ApplicationEntity application =
        saveApplication(ApplicationState.DRAFT, false, Instant.now());
    setTimestamps(application.getId(), "created_at", CUTOFF.minus(365, ChronoUnit.DAYS));

    // Act
    final List<ApplicationEntity> applications = findExpiredDrafts();

    // Assert
    assertThat(applications).isEmpty();
  }

  @Test
  void whenCompletedAndModifiedBeforeCutoff_thenNotReturned() {
    // Arrange
    saveApplication(ApplicationState.COMPLETED, false, CUTOFF.minus(1, ChronoUnit.DAYS));

    // Act
    final List<ApplicationEntity> applications = findExpiredDrafts();

    // Assert
    assertThat(applications).isEmpty();
  }

  @Test
  void whenDraftModifiedBeforeCutoffAndAlreadyDeleted_thenNotReturned() {
    // Arrange
    saveApplication(ApplicationState.DRAFT, true, CUTOFF.minus(1, ChronoUnit.DAYS));

    // Act
    final List<ApplicationEntity> applications = findExpiredDrafts();

    // Assert
    assertThat(applications).isEmpty();
  }

  private List<ApplicationEntity> findExpiredDrafts() {
    clearCache();
    return applicationRepository.findAll(ApplicationSpecification.findByExpiredDraft(CUTOFF));
  }

  private ApplicationEntity saveApplication(
      ApplicationState state, boolean deleted, Instant modifiedAt) {
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .applicationState(state)
                    .deleted(deleted)
                    .deletedAt(deleted ? Instant.now() : null));
    applicationRepository.saveAndFlush(application);
    setTimestamps(application.getId(), "modified_at", modifiedAt);
    return application;
  }

  private void setTimestamps(UUID applicationId, String column, Instant value) {
    entityManager
        .createNativeQuery("UPDATE applications SET " + column + " = :value WHERE id = :id")
        .setParameter("value", value)
        .setParameter("id", applicationId)
        .executeUpdate();
  }
}
