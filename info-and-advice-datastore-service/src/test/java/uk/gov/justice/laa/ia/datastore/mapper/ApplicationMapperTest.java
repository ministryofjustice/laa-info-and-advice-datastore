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
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceEntityGenerator;
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
              builder.clientDetails(
                  ClientDetailsEntityGenerator.createWithId(
                      clientDetailsBuilder -> {
                        clientDetailsBuilder.address(AddressEntityGenerator.createWithId(null));
                      }));
              builder.evidence(EvidenceEntityGenerator.createWithId(null));
              builder.declaration(DeclarationEntityGenerator.createWithId(null));
            });

    final ApplicationResponse mappedModel = sut.toApplication(application);

    assertEquals(application.getId(), mappedModel.getId());
    assertEquals(application.getProviderFirmId(), mappedModel.getProviderFirmId());
    assertEquals(application.getProviderOfficeId(), mappedModel.getProviderOfficeId());
    assertEquals(application.getMeansAssessmentId(), mappedModel.getMeansAssessmentStatus());
    assertEquals(application.getApplicationState(), mappedModel.getApplicationState());
    assertEquals(application.getReasonForReapplication(), mappedModel.getReasonForReapplication());
    assertEquals(
        application.getMeansAssessmentRequired(), mappedModel.getMeansAssessmentRequired());
    assertEquals(application.getTypeOfNonMeans(), mappedModel.getTypeOfNonMeans());
    assertEquals(application.getEcfFlag(), mappedModel.getEcfFlag());
    assertEquals(application.getContribution(), mappedModel.getContribution());
    assertEquals(application.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(application.getCreatedBy(), mappedModel.getCreatedBy());
    assertEquals(application.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
    assertEquals(application.getModifiedBy(), mappedModel.getModifiedBy());
    assertEquals(application.getClientDetails().getId(), mappedModel.getIndividualLegalAidNumber());
    assertNotNull(mappedModel.getClient());
    assertEquals(
        application.getClientDetails().getFullName(), mappedModel.getClient().getFullName());
    assertEquals(
        application.getClientDetails().getDateOfBirth(), mappedModel.getClient().getDateOfBirth());
    assertNotNull(mappedModel.getClient().getAddress());
    assertEquals(
        application.getClientDetails().getAddress().getAddressLine1(),
        mappedModel.getClient().getAddress().getAddressLine1());
  }

  @Test
  void toApplication_whenOptionalFieldsAreNull_thenShouldMapNull() {
    final ApplicationResponse mappedModel =
        sut.toApplication(
            ApplicationEntityGenerator.createWithId(
                builder -> {
                  builder.clientDetails(
                      ClientDetailsEntityGenerator.createWithId(
                          clientDetailsBuilder -> {
                            clientDetailsBuilder.address(AddressEntityGenerator.createWithId(null));
                          }));
                  builder.meansAssessmentId(null);
                }));

    assertNull(mappedModel.getMeansAssessmentStatus());
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
    assertNotNull(mappedModel.getClientDetails());
  }
}
