package uk.gov.justice.laa.ia.datastore.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;

/** Generator for EligibilityResultEntity. */
@UtilityClass
public class EligibilityResultEntityGenerator {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create an EligibilityResultEntity without an ID.
   *
   * @param customizer consumer to customize the entity
   * @return the entity
   */
  public static EligibilityResultEntity createWithoutId(
      Consumer<EligibilityResultEntity.EligibilityResultEntityBuilder> customizer) {

    JsonNode dummyJson = objectMapper.createObjectNode().put("test", "data");

    var builder =
        EligibilityResultEntity.builder().applicationId(UUID.randomUUID()).resultJson(dummyJson);

    if (customizer != null) {
      customizer.accept(builder);
    }

    return builder.build();
  }

  /** Create a basic EligibilityResultEntity. */
  public static EligibilityResultEntity createEligibilityResult(
      UUID applicationId, String status, int score) {
    JsonNode resultJson =
        new ObjectMapper().createObjectNode().put("status", status).put("score", score);
    return EligibilityResultEntityGenerator.createWithoutId(
        builder -> {
          builder.applicationId(applicationId).resultJson(resultJson).createdAt(Instant.now());
        });
  }
}
