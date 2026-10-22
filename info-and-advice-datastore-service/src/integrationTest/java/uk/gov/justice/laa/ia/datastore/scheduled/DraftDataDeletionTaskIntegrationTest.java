package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the DraftDataDeletionTask scheduled task. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class DraftDataDeletionTaskIntegrationTest extends BaseIntegrationTest {

  private static final int DRAFT_RETENTION_YEARS_OFFSET = 2;
  private static final Instant OLDER_THAN_RETENTION = Instant.now().minus(3 * 365, ChronoUnit.DAYS);

  private DraftDataDeletionTask sut;

  @BeforeEach
  void setUpTask() {
    // The trigger resets modified_at on every update. Postgres DDL is transactional, so this is
    // rolled back with the test transaction.
    entityManager
        .createNativeQuery(
            "ALTER TABLE applications DISABLE TRIGGER application_modified_at_trigger")
        .executeUpdate();
    final SystemDrivenEventService systemDrivenEventService =
        new SystemDrivenEventService(eventRepository, objectMapper, new AuditLogger());
    this.sut =
        new DraftDataDeletionTask(
            applicationRepository,
            DRAFT_RETENTION_YEARS_OFFSET,
            new ApplicationDataDeletionService(
                applicationRepository, eventRepository, systemDrivenEventService));
  }

  @Test
  void givenDraftNotModifiedWithinRetentionPeriod_thenDeletePiiAndMarkApplicationDeleted() {
    // Arrange
    final ApplicationEntity application = saveApplication(ApplicationState.DRAFT, false);
    backdateModifiedAt(application.getId(), OLDER_THAN_RETENTION);

    // Act
    sut.run();

    // Assert
    final var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertTrue(savedApplication.isDeleted());
    assertNotNull(savedApplication.getDeletedAt());
    assertNull(savedApplication.getClientDetails().getFirstName());
    assertNull(savedApplication.getClientDetails().getLastName());
    assertNull(savedApplication.getClientDetails().getDateOfBirth());
    assertNull(savedApplication.getClientDetails().getNiNumber());

    final var events = eventRepository.findByApplicationId(application.getId());
    assertEquals(1, events.size());
    final EventEntity event = events.getFirst();
    assertEquals(DraftDataDeletionTask.class.getSimpleName(), event.getUrlPath());
    assertTrue(event.getPayload().get("deleted").asBoolean());
  }

  @Test
  void givenDraftModifiedWithinRetentionPeriod_thenDoNothing() {
    // Arrange
    final ApplicationEntity application = saveApplication(ApplicationState.DRAFT, false);

    // Act
    sut.run();

    // Assert
    assertUntouched(application.getId());
  }

  @Test
  void givenCompletedApplicationNotModifiedWithinRetentionPeriod_thenDoNothing() {
    // Arrange
    final ApplicationEntity application = saveApplication(ApplicationState.COMPLETED, false);
    backdateModifiedAt(application.getId(), OLDER_THAN_RETENTION);

    // Act
    sut.run();

    // Assert
    assertUntouched(application.getId());
  }

  @Test
  void givenDraftAlreadyDeleted_thenDoNotProcessItAgain() {
    // Arrange
    final ApplicationEntity application = saveApplication(ApplicationState.DRAFT, true);
    backdateModifiedAt(application.getId(), OLDER_THAN_RETENTION);

    // Act
    sut.run();

    // Assert
    assertTrue(eventRepository.findByApplicationId(application.getId()).isEmpty());
  }

  private ApplicationEntity saveApplication(ApplicationState state, boolean deleted) {
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .applicationState(state)
                    .deleted(deleted)
                    .deletedAt(deleted ? Instant.now() : null));
    return applicationRepository.saveAndFlush(application);
  }

  private void backdateModifiedAt(UUID applicationId, Instant modifiedAt) {
    entityManager
        .createNativeQuery("UPDATE applications SET modified_at = :value WHERE id = :id")
        .setParameter("value", modifiedAt)
        .setParameter("id", applicationId)
        .executeUpdate();
    clearCache();
  }

  private void assertUntouched(UUID applicationId) {
    final var savedApplication = applicationRepository.findById(applicationId).orElseThrow();
    assertFalse(savedApplication.isDeleted());
    assertNull(savedApplication.getDeletedAt());
    assertEquals("Joe", savedApplication.getClientDetails().getFirstName());
    assertTrue(eventRepository.findByApplicationId(applicationId).isEmpty());
  }
}
