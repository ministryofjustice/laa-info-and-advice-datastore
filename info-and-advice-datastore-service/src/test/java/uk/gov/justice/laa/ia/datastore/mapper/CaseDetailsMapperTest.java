package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.CaseDetailsEntity;
import uk.gov.justice.laa.ia.datastore.generator.CaseDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.CaseDetailsResponse;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class CaseDetailsMapperTest {
  @InjectMocks private final CaseDetailsMapper sut = new CaseDetailsMapperImpl();
  @Spy private final DateTimeMapper dateTimeMapper = new DateTimeMapperImpl();

  @Test
  void toCaseDetailsResponse_shouldMapAllProperties() {
    final CaseDetailsEntity caseDetails =
        CaseDetailsEntityGenerator.createWithId(UUID.randomUUID(), null);

    final CaseDetailsResponse mappedModel = sut.toCaseDetailsResponse(caseDetails);
    assertEquals(caseDetails.getId(), mappedModel.getId());
    assertEquals(caseDetails.getReferenceNumber(), mappedModel.getReferenceNumber());
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
}
