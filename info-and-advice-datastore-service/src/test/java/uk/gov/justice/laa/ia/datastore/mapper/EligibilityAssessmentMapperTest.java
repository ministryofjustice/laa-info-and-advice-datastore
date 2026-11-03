package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.ia.datastore.config.JacksonConfig;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityAssessmentEntityGenerator;

/** Test class for {@link EligibilityAssessmentMapper}. */
@SpringBootTest(
    classes = {
      EligibilityAssessmentMapperImpl.class,
      DateTimeMapperImpl.class,
      JsonNodeMapperImpl.class,
      JacksonConfig.class
    })
public class EligibilityAssessmentMapperTest {
  @Autowired private EligibilityAssessmentMapper eligibilityAssessmentMapper;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void toEligibility_shouldMapAllProperties() {
    final var entity =
        EligibilityAssessmentEntityGenerator.createWithoutId(
            builder -> builder.resultJson(objectMapper.createObjectNode().put("success", false)));

    final var mappedModel = eligibilityAssessmentMapper.toEligibilityAssessment(entity);

    assertNotNull(mappedModel.getData());
    assertEquals(
        entity.getData().get("client_age").asText(), mappedModel.getData().getClientAge().get());
    assertFalse(mappedModel.getResult().getSuccess().get());
  }
}
