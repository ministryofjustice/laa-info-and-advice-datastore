package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;

/** Test class for {@link EligibilityMapper}. */
public class EligibilityMapperTest extends BaseMapperTest {
  @Test
  void toEligibilityResult_shouldMapAllProperties() {
    final var entity = EligibilityResultEntityGenerator.createWithoutId(null);

    final var mappedModel = eligibilityMapper.toEligibilityResult(entity);

    assertEquals(entity.getApplicationId(), mappedModel.getApplicationId());
    assertEquals(entity.getCreatedAt(), mappedModel.getCreatedAt());
    assertEquals(entity.getEligibilityResultId(), mappedModel.getEligibilityResultId());
    assertEquals(entity.getResultJson(), mappedModel.getEligibilityResult());
  }
}
