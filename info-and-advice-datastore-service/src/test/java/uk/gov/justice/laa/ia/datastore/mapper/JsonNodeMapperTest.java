package uk.gov.justice.laa.ia.datastore.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for {@link JsonNodeMapper}. */
public class JsonNodeMapperTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private JsonNodeMapper sut;

  @BeforeEach
  void setUp() {
    sut = new JsonNodeMapperImpl();
    sut.objectMapper = objectMapper;
  }

  @Test
  void toObject_whenNull_shouldReturnNull() {
    assertNull(sut.toObject(null));
  }

  @Test
  void toObject_shouldConvertObjectNodeToPlainMap() {
    final JsonNode node = objectMapper.createObjectNode().put("question", "answer");

    final Object result = sut.toObject(node);

    assertThat(result).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) result;
    assertThat(map).containsExactly(Map.entry("question", "answer"));
  }

  @Test
  void toObject_shouldConvertArrayNodeToPlainList() {
    final JsonNode node = objectMapper.createArrayNode().add("first").add("second");

    final Object result = sut.toObject(node);

    assertThat(result).isInstanceOf(List.class);
    @SuppressWarnings("unchecked")
    final List<Object> list = (List<Object>) result;
    assertThat(list).containsExactly("first", "second");
  }

  @Test
  void toObject_resultMustSerializeAsPlainJson_notAsJsonNodeBeanProperties() throws Exception {
    final JsonNode node = objectMapper.createObjectNode().put("question", "answer");

    final Object result = sut.toObject(node);

    assertThat(objectMapper.writeValueAsString(result)).isEqualTo("{\"question\":\"answer\"}");
  }
}
