package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.CaseDetailsEntity;
import uk.gov.justice.laa.ia.datastore.generator.CaseDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CreateCaseDetailsCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.CaseDetailsResponse;
import uk.gov.justice.laa.ia.datastore.model.CreateCaseDetailsCommand;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class CaseDetailsMapperTest extends BaseMapperTest {
  private final CaseDetailsMapper sut;

  CaseDetailsMapperTest() {
    sut = caseDetailsMapper;
  }

  @Test
  void toCaseDetailsResponse_shouldMapAllProperties() {
    final CaseDetailsEntity caseDetails = CaseDetailsEntityGenerator.createWithId(null);

    final CaseDetailsResponse mappedModel = sut.toCaseDetailsResponse(caseDetails);
    assertEquals(caseDetails.getId(), mappedModel.getId());
    assertEquals(caseDetails.getRequireEcf(), mappedModel.getRequireEcf());
    assertEquals(caseDetails.getHasPreviousLegalAid(), mappedModel.getHasPreviousLegalAid());
    assertEquals(caseDetails.getHasSixMonthsLegalHelp(), mappedModel.getHasSixMonthsLegalHelp());
    assertEquals(
        caseDetails.getMeansAssessmentRequired(), mappedModel.getMeansAssessmentRequired());
    assertEquals(caseDetails.getTypeNonMeansTested(), mappedModel.getTypeNonMeansTested());
    assertEquals(caseDetails.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(caseDetails.getCreatedBy(), mappedModel.getCreatedBy());
    assertEquals(caseDetails.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
    assertEquals(caseDetails.getModifiedBy(), mappedModel.getModifiedBy());
  }

  @Test
  void createCaseDetailsCommand_toCaseDetailsEntity_shouldMapProperties() {
    final CreateCaseDetailsCommand cmd = CreateCaseDetailsCommandGenerator.create(null);

    final CaseDetailsEntity mappedModel = sut.toCaseDetailsEntity(cmd);

    assertEquals(cmd.getHasLegalHelpLastSixMonths(), mappedModel.getHasSixMonthsLegalHelp());
    assertEquals(cmd.getIsTypeNonMeansTested(), mappedModel.getTypeNonMeansTested());
    assertEquals(cmd.getMeansAssessmentRequired(), mappedModel.getMeansAssessmentRequired());
    assertEquals(cmd.getRequiresEcf(), mappedModel.getRequireEcf());
    assertEquals(cmd.getHasPreviousLegalAid(), mappedModel.getHasPreviousLegalAid());
  }
}
