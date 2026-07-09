package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;

/** Test class for {@link EligibilityMapper}. */
@SpringBootTest
public class EligibilityMapperTest {
  @Autowired private EligibilityMapper eligibilityMapper;

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
