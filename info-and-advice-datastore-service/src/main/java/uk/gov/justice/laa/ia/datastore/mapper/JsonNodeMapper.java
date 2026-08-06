package uk.gov.justice.laa.ia.datastore.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

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
}
