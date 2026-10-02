package uk.gov.justice.laa.ia.datastore.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.config.PiiRedactionProperties;

/**
 * Replaces configured PII field values anywhere in a JSON payload with generated UUIDs, returning
 * the extracted values keyed by those UUIDs so they can be looked up/reconstructed later.
 */
@Component
public class PiiRedactor {

  // Used for null/blank PII values instead of minting a UUID, since there is nothing to
  // reconstruct.
  private static final String EMPTY_UUID = "00000000-0000-0000-0000-000000000000";

  private final Set<String> piiFields;
  private final ObjectMapper objectMapper;

  public PiiRedactor(PiiRedactionProperties properties, ObjectMapper objectMapper) {
    this.piiFields = new HashSet<>(properties.fields());
    this.objectMapper = objectMapper;
  }

  /**
   * Redacts configured PII fields out of {@code payload} in place.
   *
   * @param payload the JSON tree to redact, mutated directly.
   * @return a map of generated UUID to the original value it replaced.
   */
  public ObjectNode redact(JsonNode payload) {
    ObjectNode piiData = objectMapper.createObjectNode();
    redactNode(payload, piiData, new HashMap<>());
    return piiData;
  }

  private void redactNode(JsonNode node, ObjectNode piiData, Map<JsonNode, String> seen) {
    if (node.isObject()) {
      ObjectNode object = (ObjectNode) node;
      List<String> fieldNames = new ArrayList<>();
      object.fieldNames().forEachRemaining(fieldNames::add);
      for (String fieldName : fieldNames) {
        JsonNode value = object.get(fieldName);
        if (piiFields.contains(fieldName) && value.isValueNode()) {
          object.set(fieldName, redactValue(value, piiData, seen));
        } else {
          redactNode(value, piiData, seen);
        }
      }
    } else if (node.isArray()) {
      for (JsonNode element : node) {
        redactNode(element, piiData, seen);
      }
    }
  }

  private JsonNode redactValue(JsonNode value, ObjectNode piiData, Map<JsonNode, String> seen) {
    if (isBlank(value)) {
      return TextNode.valueOf(EMPTY_UUID);
    }
    String uuid =
        seen.computeIfAbsent(
            value,
            v -> {
              String id = UUID.randomUUID().toString();
              piiData.set(id, v);
              return id;
            });
    return TextNode.valueOf(uuid);
  }

  private boolean isBlank(JsonNode value) {
    return value.isNull() || (value.isTextual() && value.asText().isBlank());
  }
}
