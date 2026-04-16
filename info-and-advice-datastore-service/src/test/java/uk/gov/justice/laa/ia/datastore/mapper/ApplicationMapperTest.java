package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ClientCaseDetailsStatus;
import uk.gov.justice.laa.ia.datastore.model.OverallApplicationStatus;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class ApplicationMapperTest {
  @InjectMocks private final ApplicationMapper sut = new ApplicationMapperImpl();
  @Spy private final DateTimeMapper dateTimeMapper = new DateTimeMapperImpl();

  @Test
  void toApplication_shouldMapAllProperties() {
    final ApplicationEntity application =
        createApplication()
            .eligibilityResultId(UUID.randomUUID())
            .meansAssessmentStatusId(UUID.randomUUID())
            .evidenceStatusId(UUID.randomUUID())
            .clientDeclarationStatusId(UUID.randomUUID())
            .build();

    final ApplicationResponse mappedModel = sut.toApplication(application);

    assertEquals(application.getId(), mappedModel.getReferenceNumber());
    assertEquals(
        application.getIndividualLegalAidNumber(), mappedModel.getIndividualLegalAidNumber());
    assertEquals(application.getProviderFirmId(), mappedModel.getProviderFirmId());
    assertEquals(application.getProviderOfficeId(), mappedModel.getProviderOfficeId());
    assertEquals(application.getEligibilityResultId(), mappedModel.getEligibilityResult());
    assertEquals(
        application.getClientCaseDetailsStatus(), mappedModel.getClientCaseDetailsStatus());
    assertEquals(application.getMeansAssessmentStatusId(), mappedModel.getMeansAssessmentStatus());
    assertEquals(application.getEvidenceStatusId(), mappedModel.getEvidenceStatus());
    assertEquals(
        application.getClientDeclarationStatusId(), mappedModel.getClientDeclarationStatus());
    assertEquals(
        application.getOverallApplicationStatus(), mappedModel.getOverallApplicationStatus());
    assertEquals(application.getUniqueFileNumber(), mappedModel.getUniqueFileNumber());
    assertEquals(application.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(application.getCreatedBy(), mappedModel.getCreatedBy());
    assertEquals(application.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
    assertEquals(application.getModifiedBy(), mappedModel.getModifiedBy());
  }

  @Test
  void toApplication_whenOptionalFieldsAreNull_thenShouldMapNull() {
    final ApplicationResponse mappedModel = sut.toApplication(createApplication().build());

    assertNull(mappedModel.getMeansAssessmentStatus());
    assertNull(mappedModel.getEvidenceStatus());
    assertNull(mappedModel.getClientDeclarationStatus());
  }

  @Test
  void toApplication_whenNull_shouldReturnNull() {
    assertNull(sut.toApplication(null));
  }

  private static ApplicationEntity.ApplicationEntityBuilder createApplication() {
    return ApplicationEntity.builder()
        .id(UUID.randomUUID())
        .individualLegalAidNumber(UUID.randomUUID())
        .providerFirmId(UUID.randomUUID())
        .providerOfficeId(UUID.randomUUID())
        .clientCaseDetailsStatus(ClientCaseDetailsStatus.DRAFT)
        .overallApplicationStatus(OverallApplicationStatus.DRAFT)
        .uniqueFileNumber(UUID.randomUUID())
        .createdAt(Instant.now())
        .createdBy("Joe Bloggs")
        .modifiedAt(Instant.now())
        .modifiedBy("James Bloggs");
  }
}
