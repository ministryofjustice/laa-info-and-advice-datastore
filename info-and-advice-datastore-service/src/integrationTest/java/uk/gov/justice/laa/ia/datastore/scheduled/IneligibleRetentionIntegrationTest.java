package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.gateway.stub.ClaimsGatewayStub;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.repository.EligibilityResultRepository;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;
import uk.gov.justice.laa.ia.datastore.service.RetentionDateUpdateService;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/**
 * End-to-end tests for the ineligible retention rule: the resolver sets the date and the deletion
 * task deletes the application, without the claims API being involved.
 */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@ExtendWith(MockitoExtension.class)
public class IneligibleRetentionIntegrationTest extends BaseIntegrationTest {

  private static final int RETENTION_YEARS_OFFSET = 3;
  private static final Instant MODIFIED_AT = Instant.parse("2020-03-01T10:00:00Z");

  @Autowired private EligibilityResultRepository eligibilityResultRepository;
  @Spy private ClaimsGatewayStub claimsGateway = new ClaimsGatewayStub();

  private DataRetentionDateResolverTask resolver;
  private DataDeletionTask deletionTask;

  @BeforeEach
  void setUpTasks() {
    // The trigger resets modified_at on every update. Postgres DDL is transactional, so this is
    // rolled back with the test transaction.
    entityManager
        .createNativeQuery(
            "ALTER TABLE applications DISABLE TRIGGER application_modified_at_trigger")
        .executeUpdate();
    final SystemDrivenEventService systemDrivenEventService =
        new SystemDrivenEventService(eventRepository, objectMapper, new AuditLogger());
    final RetentionDateUpdateService retentionDateUpdateService =
        new RetentionDateUpdateService(applicationRepository, systemDrivenEventService);
    this.resolver =
        new DataRetentionDateResolverTask(
            claimsGateway,
            RETENTION_YEARS_OFFSET,
            RETENTION_YEARS_OFFSET,
            applicationRepository,
            retentionDateUpdateService);
    this.deletionTask =
        new DataDeletionTask(
            applicationRepository,
            claimsGateway,
            RETENTION_YEARS_OFFSET,
            retentionDateUpdateService,
            new ApplicationDataDeletionService(
                applicationRepository, eventRepository, systemDrivenEventService));
  }

  @Test
  void givenIneligibleCompletedApplication_thenDateIsSetFromModifiedAtAndApplicationIsDeleted() {
    // Arrange
    final ApplicationEntity application = saveCompletedApplication(false, MODIFIED_AT);
    final Instant expectedDate =
        MODIFIED_AT.atOffset(ZoneOffset.UTC).plusYears(RETENTION_YEARS_OFFSET).toInstant();

    // Act
    resolver.run();
    clearCache();

    // Assert: the retention date is set, with an audit event that isn't claim-based
    final var withDate = applicationRepository.findById(application.getId()).orElseThrow();
    assertEquals(expectedDate, withDate.getDataRetentionDate());
    assertFalse(withDate.isDeleted());
    final var dateEvents = eventRepository.findByApplicationId(application.getId());
    assertEquals(1, dateEvents.size());
    final var dateEvent = dateEvents.getFirst();
    assertEquals(DataRetentionDateResolverTask.class.getSimpleName(), dateEvent.getUrlPath());
    assertEquals(expectedDate.toString(), dateEvent.getPayload().get("dataRetentionDate").asText());
    assertFalse(dateEvent.getPayload().has("claimsId"));

    // Act
    deletionTask.run();
    clearCache();

    // Assert: the expired date leads to deletion
    final var deleted = applicationRepository.findById(application.getId()).orElseThrow();
    assertTrue(deleted.isDeleted());
    assertNotNull(deleted.getDeletedAt());
    assertNull(deleted.getClientDetails().getFirstName());
    final var events = eventRepository.findByApplicationId(application.getId());
    assertEquals(2, events.size());
    assertTrue(
        events.stream()
            .map(EventEntity::getUrlPath)
            .anyMatch(DataDeletionTask.class.getSimpleName()::equals));
    verifyNoInteractions(claimsGateway);
  }

  @Test
  void givenIneligibleCompletedApplicationModifiedRecently_thenDateIsSetButNotDeleted() {
    // Arrange
    final Instant modifiedAt = Instant.now();
    final ApplicationEntity application = saveCompletedApplication(false, modifiedAt);

    // Act
    resolver.run();
    deletionTask.run();
    clearCache();

    // Assert
    final var saved = applicationRepository.findById(application.getId()).orElseThrow();
    assertNotNull(saved.getDataRetentionDate());
    assertTrue(saved.getDataRetentionDate().isAfter(Instant.now()));
    assertFalse(saved.isDeleted());
    assertEquals("Joe", saved.getClientDetails().getFirstName());
    verifyNoInteractions(claimsGateway);
  }

  @Test
  void givenEligibleCompletedApplicationWithoutClaim_thenNoDateIsSet() {
    // Arrange
    final ApplicationEntity application = saveCompletedApplication(true, MODIFIED_AT);

    // Act
    resolver.run();
    clearCache();

    // Assert
    final var saved = applicationRepository.findById(application.getId()).orElseThrow();
    assertNull(saved.getDataRetentionDate());
  }

  @Test
  void givenSpecificationQuery_thenEligibilityResultsAreEagerlyLoaded() {
    // Arrange
    saveCompletedApplication(false, MODIFIED_AT);
    clearCache();

    // Act
    final var applications =
        applicationRepository.findAll(
            ApplicationSpecification.findByMissingDataRetentionDateAndStatus());

    // Assert: readable without an open session, as in scheduled execution
    assertEquals(1, applications.size());
    assertTrue(Hibernate.isInitialized(applications.getFirst().getEligibilityResults()));
  }

  private ApplicationEntity saveCompletedApplication(boolean eligible, Instant modifiedAt) {
    final ApplicationEntity application =
        applicationRepository.saveAndFlush(
            ApplicationEntityGenerator.createWithoutId(
                builder ->
                    builder
                        .withDefaultClientDetails()
                        .ufn(ClaimsGatewayStub.NO_CLAIMS_UFN)
                        .dataRetentionDate(null)
                        .applicationState(ApplicationState.COMPLETED)
                        .providerOfficeCode(ClaimsGatewayStub.FIXTURE_OFFICE_CODE)));
    final EligibilityResultEntity result =
        EligibilityResultEntityGenerator.createEligibilityResult(application.getId(), "RESULT", 1);
    result.setIndication(eligible);
    eligibilityResultRepository.saveAndFlush(result);
    backdateModifiedAt(application.getId(), modifiedAt);
    return application;
  }

  private void backdateModifiedAt(UUID applicationId, Instant modifiedAt) {
    entityManager
        .createNativeQuery("UPDATE applications SET modified_at = :value WHERE id = :id")
        .setParameter("value", modifiedAt)
        .setParameter("id", applicationId)
        .executeUpdate();
    clearCache();
  }
}
