package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.EvidenceResponse;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class EvidenceMapperTest {
  @InjectMocks private final EvidenceMapper sut = new EvidenceMapperImpl();
  @Spy private final DateTimeMapper dateTimeMapper = new DateTimeMapperImpl();

  @Test
  void toEvidenceResponse_shouldMapAllProperties() {
    final EvidenceEntity evidence = EvidenceEntityGenerator.createWithId(UUID.randomUUID(), null);

    final EvidenceResponse mappedModel = sut.toEvidenceResponse(evidence);

    assertEquals(evidence.getId(), mappedModel.getId());
    assertEquals(evidence.getReferenceNumber(), mappedModel.getReferenceNumber());
    assertEquals(evidence.getEvidenceStatus(), mappedModel.getEvidenceStatus());
    assertEquals(evidence.isPayeIncomeEvidence(), mappedModel.getPayeIncomeEvidence());
    assertEquals(evidence.isOtherIncomeEvidence(), mappedModel.getOtherIncomeEvidence());
    assertEquals(evidence.isHousingCostsEvidence(), mappedModel.getHousingCostsEvidence());
    assertEquals(evidence.isCapitalEvidence(), mappedModel.getCapitalEvidence());
    assertEquals(evidence.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(evidence.getCreatedBy(), mappedModel.getCreatedBy());
    assertEquals(evidence.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
    assertEquals(evidence.getModifiedBy(), mappedModel.getModifiedBy());
  }
}
