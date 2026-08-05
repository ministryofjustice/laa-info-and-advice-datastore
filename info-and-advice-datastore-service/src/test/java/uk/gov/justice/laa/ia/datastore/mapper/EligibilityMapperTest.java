package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;

/** Test class for {@link EligibilityMapper}. */
@SpringBootTest(classes = {EligibilityMapperImpl.class, DateTimeMapperImpl.class})
public class EligibilityMapperTest {
  @Autowired private EligibilityMapper eligibilityMapper;

  @Test
  void toEligibilityResult_shouldMapAllProperties() {
    final var entity = EligibilityResultEntityGenerator.createWithoutId(null);

    final var mappedModel = eligibilityMapper.toEligibilityResult(entity);

    assertEquals(entity.getData(), mappedModel.getData());
    assertEquals(entity.getResultJson(), mappedModel.getResult());
  }
}
