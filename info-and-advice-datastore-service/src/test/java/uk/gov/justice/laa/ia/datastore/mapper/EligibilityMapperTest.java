package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.ia.datastore.config.JacksonConfig;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;

/** Test class for {@link EligibilityMapper}. */
@SpringBootTest(
    classes = {
      EligibilityMapperImpl.class,
      DateTimeMapperImpl.class,
      JsonNodeMapperImpl.class,
      JacksonConfig.class
    })
public class EligibilityMapperTest {
  @Autowired private EligibilityMapper eligibilityMapper;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void toEligibilityResult_shouldMapAllProperties() {
    final var entity = EligibilityResultEntityGenerator.createWithoutId(null);

    final var mappedModel = eligibilityMapper.toEligibilityResult(entity);

    assertEquals(entity.getData(), objectMapper.valueToTree(mappedModel.getData()));
    assertEquals(entity.getResultJson(), objectMapper.valueToTree(mappedModel.getResult()));
  }
}
