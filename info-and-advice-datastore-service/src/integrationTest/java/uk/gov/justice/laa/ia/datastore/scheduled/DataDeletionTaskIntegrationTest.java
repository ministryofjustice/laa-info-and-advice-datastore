package uk.gov.justice.laa.ia.datastore.scheduled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.gateway.stub.ClaimsGatewayStub;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.service.ApplicationDataDeletionService;
import uk.gov.justice.laa.ia.datastore.service.RetentionDateUpdateService;
import uk.gov.justice.laa.ia.datastore.service.SystemDrivenEventService;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the DataDeletionTask scheduled task. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
@ExtendWith(MockitoExtension.class)
public class DataDeletionTaskIntegrationTest extends BaseIntegrationTest {

  private DataDeletionTask sut;
  @Spy private ClaimsGatewayStub claimsGateway = new ClaimsGatewayStub();
  static final String officeCode = ClaimsGatewayStub.FIXTURE_OFFICE_CODE;
  static final int DATA_RETENTION_YEARS_OFFSET = 3;

  @BeforeEach
  void setUpTask() {
    final SystemDrivenEventService systemDrivenEventService =
        new SystemDrivenEventService(eventRepository, objectMapper, new AuditLogger());
    this.sut =
        new DataDeletionTask(
            applicationRepository,
            claimsGateway,
            DATA_RETENTION_YEARS_OFFSET,
            new RetentionDateUpdateService(applicationRepository, systemDrivenEventService),
            new ApplicationDataDeletionService(
                applicationRepository, eventRepository, systemDrivenEventService));
  }

  @Test
  void givenExpiredApplicationWithNoClaims_thenLogErrorAndDoNothing() {
    // Arrange
    final String ufn = ClaimsGatewayStub.NO_CLAIMS_UFN;
    final Instant expiredRetentionDate = Instant.now().minus(1, ChronoUnit.DAYS);
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(expiredRetentionDate)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(application);

    // Act
    sut.run();

    // Assert
    var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertFalse(savedApplication.isDeleted());
    assertNull(savedApplication.getDeletedAt());
    assertEquals(expiredRetentionDate, savedApplication.getDataRetentionDate());
    assertEquals("Joe", savedApplication.getClientDetails().getFirstName());
    assertTrue(eventRepository.findByApplicationId(application.getId()).isEmpty());
  }

  @Test
  void givenExpiredApplicationWithNoNewClaims_thenDeletePiiAndMarkApplicationDeleted() {
    // Arrange
    final String ufn = ClaimsGatewayStub.OLD_CLAIM_UFN;
    final Instant expiredRetentionDate = Instant.now().minus(1, ChronoUnit.DAYS);
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(expiredRetentionDate)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(application);

    // Act
    sut.run();

    // Assert
    var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertTrue(savedApplication.isDeleted());
    assertNotNull(savedApplication.getDeletedAt());
    assertNull(savedApplication.getClientDetails().getFirstName());
    assertNull(savedApplication.getClientDetails().getLastName());
    assertNull(savedApplication.getClientDetails().getDateOfBirth());
    assertNull(savedApplication.getClientDetails().getNiNumber());

    var events = eventRepository.findByApplicationId(application.getId());
    assertEquals(1, events.size());
    EventEntity event = events.get(0);
    assertEquals(DataDeletionTask.class.getSimpleName(), event.getUrlPath());
    assertTrue(event.getPayload().get("deleted").asBoolean());
  }

  @Test
  void givenExpiredApplicationWithNewClaims_thenUpdateDataRetentionDate() {
    // Arrange
    final String ufn = ClaimsGatewayStub.RECENT_CLAIM_UFN;
    final Instant expiredRetentionDate = Instant.now().minus(1, ChronoUnit.DAYS);
    final Instant expectedDataRetentionDate =
        ClaimsGatewayStub.RECENT_CLAIM_UPDATED_ON
            .plus(DATA_RETENTION_YEARS_OFFSET, ChronoUnit.YEARS)
            .toInstant();
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .ufn(ufn)
                    .dataRetentionDate(expiredRetentionDate)
                    .applicationState(ApplicationState.COMPLETED)
                    .providerOfficeCode(officeCode));
    applicationRepository.saveAndFlush(application);

    // Act
    sut.run();

    // Assert
    var savedApplication = applicationRepository.findById(application.getId()).orElseThrow();
    assertFalse(savedApplication.isDeleted());
    assertEquals(expectedDataRetentionDate, savedApplication.getDataRetentionDate());
    assertEquals("Joe", savedApplication.getClientDetails().getFirstName());

    var events = eventRepository.findByApplicationId(application.getId());
    assertEquals(1, events.size());
    EventEntity event = events.get(0);
    assertEquals(
        expectedDataRetentionDate.toString(), event.getPayload().get("dataRetentionDate").asText());
    assertEquals(
        ClaimsGatewayStub.RECENT_CLAIM_ID.toString(), event.getPayload().get("claimsId").asText());
  }
}
