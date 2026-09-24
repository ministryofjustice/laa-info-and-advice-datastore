package uk.gov.justice.laa.ia.datastore.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.model.EligibilityData;

/**
 * Converts Jackson {@link JsonNode} entity values into plain Java objects (maps/lists/primitives)
 * for API responses, so they serialize correctly regardless of which Jackson major version the HTTP
 * layer uses.
 */
@Mapper(componentModel = "spring")
public abstract class JsonNodeMapper {

  @Autowired protected ObjectMapper objectMapper;

  /** Converts a {@link JsonNode} to a plain {@link Object}. */
  public Object toObject(JsonNode node) {
    return node == null ? null : objectMapper.convertValue(node, Object.class);
  }

  /**
   * Converts a {@link JsonNode} to the typed {@link EligibilityData} means-assessment schema.
   * Declared explicitly so MapStruct uses it (rather than generating an empty bean) when mapping
   * {@code EligibilityResultEntity.data} onto {@code EligibilityResult.data}. Unknown properties
   * are ignored (rather than failing) since older/legacy stored payloads may not yet match every
   * field of the current schema.
   */
  public EligibilityData toEligibilityData(JsonNode node) {
    if (node == null) {
      return null;
    }
    return objectMapper
        .copy()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .convertValue(node, EligibilityData.class);
  }
}
