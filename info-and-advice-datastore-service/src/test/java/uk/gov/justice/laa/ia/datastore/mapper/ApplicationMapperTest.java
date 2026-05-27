package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.IndividualEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.StartCaseCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class ApplicationMapperTest extends BaseMapperTest {
  private final ApplicationMapper sut;

  ApplicationMapperTest() {
    sut = applicationMapper;
  }

  @Test
  void toApplication_shouldMapAllProperties() {
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> {
              builder.individual(
                  IndividualEntityGenerator.createWithId(
                      individualBuilder -> {
                        individualBuilder.address(AddressEntityGenerator.createWithId(null));
                      }));
              builder.evidence(EvidenceEntityGenerator.createWithId(null));
              builder.declaration(DeclarationEntityGenerator.createWithId(null));
            });

    final ApplicationResponse mappedModel = sut.toApplication(application);

    assertEquals(application.getId(), mappedModel.getReferenceNumber());
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
    assertEquals(application.getIndividual().getId(), mappedModel.getIndividualLegalAidNumber());
  }

  @Test
  void toApplication_whenOptionalFieldsAreNull_thenShouldMapNull() {
    final ApplicationResponse mappedModel =
        sut.toApplication(
            ApplicationEntityGenerator.createWithId(
                builder -> {
                  builder.individual(
                      IndividualEntityGenerator.createWithId(
                          individualBuilder -> {
                            individualBuilder.address(AddressEntityGenerator.createWithId(null));
                          }));
                  builder.meansAssessmentStatusId(null);
                  builder.evidenceStatusId(null);
                  builder.clientDeclarationStatusId(null);
                }));

    assertNull(mappedModel.getMeansAssessmentStatus());
    assertNull(mappedModel.getEvidenceStatus());
    assertNull(mappedModel.getClientDeclarationStatus());
  }

  @Test
  void toApplication_whenNull_shouldReturnNull() {
    assertNull(sut.toApplication(null));
  }

  @Test
  void startCaseCommand_toApplication_shouldMapProperties() {
    final StartCaseCommand cmd = StartCaseCommandGenerator.create(null);

    final ApplicationEntity mappedModel = sut.toApplicationEntity(cmd);

    assertNotNull(mappedModel);
    assertNotNull(mappedModel.getIndividual());
  }
}
