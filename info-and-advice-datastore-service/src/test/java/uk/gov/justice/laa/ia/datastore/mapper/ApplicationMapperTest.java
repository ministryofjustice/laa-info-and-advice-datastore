package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceGenerator;
import uk.gov.justice.laa.ia.datastore.generator.StartCaseCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationSummary;
import uk.gov.justice.laa.ia.datastore.model.EligibilityResultResponse;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(
    classes = {
      ApplicationMapperImpl.class,
      UserContext.class,
      DeclarationMapperImpl.class,
      EligibilityMapperImpl.class,
      ClientDetailsMapperImpl.class,
      AddressMapperImpl.class,
      DateTimeMapperImpl.class,
    })
public class ApplicationMapperTest {
  @Autowired private ApplicationMapper sut;
  @MockitoBean private UserContext userContext;

  @Test
  void toApplicationSummary_shouldMapSummaryFields() {
    final ApplicationEntity application =
        ApplicationEntityGenerator.createWithId(
            builder -> {
              builder.clientDetails(ClientDetailsEntityGenerator.createWithId(null));
              builder.referenceNumber("LAA-TEST-001");
            });

    final ApplicationSummary summary = sut.toApplicationSummary(application);

    assertEquals(application.getId(), summary.getId());
    assertEquals(application.getReferenceNumber(), summary.getReferenceNumber());
    assertEquals(application.getClientDetails().getFirstName(), summary.getClientFirstName());
    assertEquals(application.getClientDetails().getLastName(), summary.getClientLastName());
    assertEquals(application.getModifiedAt(), summary.getModifiedAt().toInstant());
  }

  @Test
  void toApplicationSummary_whenNull_shouldReturnNull() {
    assertNull(sut.toApplicationSummary(null));
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
              builder.evidence(EvidenceGenerator.createEvidenceMap());
              builder.declaration(DeclarationEntityGenerator.createWithId(null));
              builder.eligibilityResults(
                  Set.of(
                      EligibilityResultEntityGenerator.createEligibilityResult(
                          UUID.randomUUID(), "ELIGIBLE", 100)));
            });

    final ApplicationResponse mappedModel = sut.toApplication(application);

    assertEquals(application.getId(), mappedModel.getId());
    assertEquals(application.getProviderFirmId(), mappedModel.getProviderFirmId());
    assertEquals(application.getProviderOfficeId(), mappedModel.getProviderOfficeId());
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
        application.getClientDetails().getFirstName(), mappedModel.getClient().getFirstName());
    assertEquals(
        application.getClientDetails().getLastName(), mappedModel.getClient().getLastName());
    assertEquals(
        application.getClientDetails().getDateOfBirth(), mappedModel.getClient().getDateOfBirth());
    assertNotNull(mappedModel.getClient().getAddress());
    assertEquals(
        application.getClientDetails().getAddress().getAddressLine1(),
        mappedModel.getClient().getAddress().getAddressLine1());
    assertEligibiltyEquals(
        application.getMostRecentEligibilityResult(), mappedModel.getEligibilityResult());
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

  @Test
  void startCaseCommand_toApplication_shouldSetCreatedAndModifiedBy() {
    // Arrange
    when(userContext.getCurrentUser()).thenReturn("USERCONTEXT:SYSTEM");
    final StartCaseCommand cmd = StartCaseCommandGenerator.create(null);

    // Act
    final ApplicationEntity mappedModel = sut.toApplicationEntity(cmd);

    // Assert
    assertEquals("USERCONTEXT:SYSTEM", mappedModel.getCreatedBy());
    assertEquals("USERCONTEXT:SYSTEM", mappedModel.getModifiedBy());
  }

  @Test
  void startCaseCommand_toApplication_shouldSetProviderFirmId() {
    // Arrange
    final UUID providerFirmId = UUID.randomUUID();
    when(userContext.getProviderFirmId()).thenReturn(providerFirmId);
    final StartCaseCommand cmd = StartCaseCommandGenerator.create(null);
    // Act
    final ApplicationEntity mappedModel = sut.toApplicationEntity(cmd);

    // Assert
    assertEquals(providerFirmId, mappedModel.getProviderFirmId());
  }

  private static void assertEligibiltyEquals(
      EligibilityResultEntity expected, EligibilityResultResponse model) {
    assertEquals(expected.getEligibilityResultId(), model.getEligibilityResultId());
    assertEquals(expected.getApplicationId(), model.getApplicationId());
    assertEquals(expected.getCreatedAt(), model.getCreatedAt().toInstant());
    assertEquals(expected.getResultJson(), model.getEligibilityResult());
  }
}
