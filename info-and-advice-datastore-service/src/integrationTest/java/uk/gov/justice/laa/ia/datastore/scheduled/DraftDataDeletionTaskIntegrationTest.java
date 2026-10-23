package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateClientDetailsCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateMeansDataCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateScopingDataCommand;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;
import uk.gov.justice.laa.ia.datastore.service.ApplicationService;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the DraftDataDeletionTask scheduled task. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class DraftDataDeletionTaskIntegrationTest extends BaseIntegrationTest {

  private static final int DRAFT_RETENTION_YEARS_OFFSET = 2;
  private static final Instant OLDER_THAN_RETENTION = Instant.now().minus(3 * 365, ChronoUnit.DAYS);

  @Autowired private ApplicationService applicationService;

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

  @ParameterizedTest
  @MethodSource("edits")
  void givenOldDraftEditedByLastModifier_thenEtagChangesAndDraftIsNotDeleted(Edit edit) {
    // Arrange
    final ApplicationEntity application = saveApplication(ApplicationState.DRAFT, false);
    backdateModifiedAt(application.getId(), OLDER_THAN_RETENTION);
    final long etagBefore = applicationRepository.findById(application.getId()).get().getEtag();
    // Restore the trigger so the edit refreshes modified_at as it does in production
    entityManager
        .createNativeQuery(
            "ALTER TABLE applications ENABLE TRIGGER application_modified_at_trigger")
        .executeUpdate();

    // Act
    final long etagAfter = edit.apply(applicationService, application.getId(), etagBefore);
    clearCache();
    sut.run();

    // Assert
    assertNotEquals(etagBefore, etagAfter);
    final var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertEquals(etagAfter, savedApplication.getEtag());
    assertFalse(savedApplication.isDeleted());
    assertNull(savedApplication.getDeletedAt());
  }

  static Stream<Named<Edit>> edits() {
    return Stream.of(
        Named.of(
            "client details",
            (service, id, etag) ->
                service
                    .updateClientDetails(
                        id,
                        UpdateClientDetailsCommand.builder().eTag(etag).firstName("Jane").build())
                    .getAsLong()),
        Named.of(
            "evidence",
            (service, id, etag) ->
                service
                    .updateEvidence(
                        id,
                        UpdateEvidenceCommand.builder()
                            .eTag(etag)
                            .evidenceExemptionCode("EXEMPT")
                            .build())
                    .getAsLong()),
        Named.of(
            "declaration",
            (service, id, etag) ->
                service
                    .updateClientDeclaration(
                        id,
                        DeclarationCommand.builder()
                            .eTag(etag)
                            .declarationConfirmation(true)
                            .build())
                    .getAsLong()),
        Named.of(
            "scoping data",
            (service, id, etag) ->
                service
                    .updateScopingData(
                        id,
                        UpdateScopingDataCommand.builder()
                            .eTag(etag)
                            .scopingQuestions(JsonNullable.of(Map.of("question", "answer")))
                            .build())
                    .getAsLong()),
        Named.of(
            "means data",
            (service, id, etag) ->
                service
                    .updateMeansData(id, UpdateMeansDataCommand.builder().eTag(etag).build())
                    .getAsLong()));
  }

  @FunctionalInterface
  interface Edit {
    long apply(ApplicationService service, UUID applicationId, long etag);
  }

  private ApplicationEntity saveApplication(ApplicationState state, boolean deleted) {
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .modifiedBy("SYSTEM")
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)
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
