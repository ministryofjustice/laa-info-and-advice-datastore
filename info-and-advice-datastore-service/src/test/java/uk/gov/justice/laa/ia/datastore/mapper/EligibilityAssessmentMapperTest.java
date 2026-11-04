package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.justice.laa.ia.datastore.config.JacksonConfig;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity;
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
  void toEligibilityAssessment_shouldMapAllProperties() throws Exception {
    final var entity = createEntityWithCfeResult();

    final var mappedModel = eligibilityAssessmentMapper.toEligibilityAssessment(entity);

    assertNotNull(mappedModel.getData());
    assertEquals(
        entity.getData().get("client_age").asText(), mappedModel.getData().getClientAge().get());
    JSONAssert.assertEquals(
        entity.getResultJson().toString(),
        objectMapper.writeValueAsString(mappedModel.getResult()),
        JSONCompareMode.STRICT);
  }

  @Test
  void toEligibilityResult_shouldMapAllProperties() throws Exception {
    final var entity = createEntityWithCfeResult();

    final var mappedModel = eligibilityAssessmentMapper.toEligibilityResult(entity);

    assertNotNull(mappedModel.getData());
    assertEquals(
        entity.getData().get("client_age").asText(), mappedModel.getData().getClientAge().get());
    JSONAssert.assertEquals(
        entity.getResultJson().toString(),
        objectMapper.writeValueAsString(mappedModel.getResult()),
        JSONCompareMode.STRICT);
  }

  private static EligibilityAssessmentEntity createEntityWithCfeResult() {
    return EligibilityAssessmentEntityGenerator.createWithoutId(
        builder -> builder.resultJson(EligibilityAssessmentEntityGenerator.createResultJson()));
  }
}
