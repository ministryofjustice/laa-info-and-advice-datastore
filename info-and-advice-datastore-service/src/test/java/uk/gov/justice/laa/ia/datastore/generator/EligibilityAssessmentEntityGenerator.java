package uk.gov.justice.laa.ia.datastore.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity;

/** Generator for EligibilityAssessmentEntity. */
@UtilityClass
public class EligibilityAssessmentEntityGenerator {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create an EligibilityAssessmentEntity without an ID.
   *
   * @param customizer consumer to customize the entity
   * @return the entity
   */
  public static EligibilityAssessmentEntity createWithoutId(
      Consumer<EligibilityAssessmentEntity.EligibilityAssessmentEntityBuilder> customizer) {

    JsonNode dummyJson = objectMapper.createObjectNode().put("success", true);
    JsonNode dummyData = objectMapper.createObjectNode().put("client_age", "18-24");

    var builder = EligibilityAssessmentEntity.builder()
        .applicationId(UUID.randomUUID())
        .data(dummyData)
        .resultJson(dummyJson);

    if (customizer != null) {
      customizer.accept(builder);
    }

    return builder.build();
  }

  /** Create a basic EligibilityAssessmentEntity. */
  public static EligibilityAssessmentEntity createEligibilityAssessment(UUID applicationId) {
    return EligibilityAssessmentEntityGenerator.createWithoutId(
        builder -> {
          builder.applicationId(applicationId).createdAt(Instant.now()).createdBy("SYSTEM");
        });
  }
}
