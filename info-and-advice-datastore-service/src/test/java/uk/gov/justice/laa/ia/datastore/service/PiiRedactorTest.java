package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.ia.datastore.config.PiiRedactionProperties;

/** Unit tests for {@link PiiRedactor}. */
class PiiRedactorTest {

  private static final String EMPTY_UUID = "00000000-0000-0000-0000-000000000000";
  private static final Pattern UUID_PATTERN =
      Pattern.compile("^[0-9a-f-]{36}$", Pattern.CASE_INSENSITIVE);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PiiRedactor sut =
      new PiiRedactor(
          new PiiRedactionProperties(List.of("firstName", "lastName", "addressLine1")),
          objectMapper);

  @Test
  void shouldRedactFlatField() throws Exception {
    ObjectNode payload = (ObjectNode) objectMapper.readTree("{\"firstName\":\"Jane\"}");

    ObjectNode piiData = sut.redact(payload);

    String uuid = payload.get("firstName").asText();
    assertThat(UUID_PATTERN.matcher(uuid).matches()).isTrue();
    assertThat(piiData.get(uuid).asText()).isEqualTo("Jane");
  }

  @Test
  void shouldRedactNestedObjectField() throws Exception {
    ObjectNode payload =
        (ObjectNode)
            objectMapper.readTree(
                "{\"client\":{\"firstName\":\"Jane\",\"unrelated\":\"keep me\"}}");

    ObjectNode piiData = sut.redact(payload);

    JsonNode client = payload.get("client");
    String uuid = client.get("firstName").asText();
    assertThat(piiData.get(uuid).asText()).isEqualTo("Jane");
    assertThat(client.get("unrelated").asText()).isEqualTo("keep me");
  }

  @Test
  void shouldRedactFieldsInArrayOfObjects() throws Exception {
    ObjectNode payload =
        (ObjectNode)
            objectMapper.readTree(
                "{\"clients\":[{\"firstName\":\"Jane\"},{\"firstName\":\"John\"}]}");

    ObjectNode piiData = sut.redact(payload);

    String janeUuid = payload.get("clients").get(0).get("firstName").asText();
    String johnUuid = payload.get("clients").get(1).get("firstName").asText();
    assertThat(piiData.get(janeUuid).asText()).isEqualTo("Jane");
    assertThat(piiData.get(johnUuid).asText()).isEqualTo("John");
  }

  @Test
  void shouldReuseSameUuidForDuplicateValuesWithinOnePayload() throws Exception {
    ObjectNode payload =
        (ObjectNode)
            objectMapper.readTree("{\"firstName\":\"Jane\",\"client\":{\"lastName\":\"Jane\"}}");

    sut.redact(payload);

    assertThat(payload.get("firstName").asText())
        .isEqualTo(payload.get("client").get("lastName").asText());
  }

  @Test
  void shouldReplaceNullValueWithEmptyUuidAndNotStoreIt() throws Exception {
    ObjectNode payload = (ObjectNode) objectMapper.readTree("{\"firstName\":null}");

    ObjectNode piiData = sut.redact(payload);

    assertThat(payload.get("firstName").asText()).isEqualTo(EMPTY_UUID);
    assertThat(piiData.has(EMPTY_UUID)).isFalse();
    assertThat(piiData).isEmpty();
  }

  @Test
  void shouldReplaceBlankValueWithEmptyUuidAndNotStoreIt() throws Exception {
    ObjectNode payload = (ObjectNode) objectMapper.readTree("{\"firstName\":\"   \"}");

    ObjectNode piiData = sut.redact(payload);

    assertThat(payload.get("firstName").asText()).isEqualTo(EMPTY_UUID);
    assertThat(piiData).isEmpty();
  }

  @Test
  void shouldLeaveNonConfiguredFieldsUntouched() throws Exception {
    ObjectNode payload =
        (ObjectNode) objectMapper.readTree("{\"caseReference\":\"ABC123\",\"eTag\":0}");

    sut.redact(payload);

    assertThat(payload.get("caseReference").asText()).isEqualTo("ABC123");
    assertThat(payload.get("eTag").asInt()).isZero();
  }

  @Test
  void shouldRecurseIntoMatchingFieldWhenValueIsContainer() throws Exception {
    // "addressLine1" happens to be an object here instead of a string - must not be stringified.
    ObjectNode payload =
        (ObjectNode) objectMapper.readTree("{\"addressLine1\":{\"firstName\":\"Jane\"}}");

    ObjectNode piiData = sut.redact(payload);

    JsonNode addressLine1 = payload.get("addressLine1");
    assertThat(addressLine1.isObject()).isTrue();
    String uuid = addressLine1.get("firstName").asText();
    assertThat(piiData.get(uuid).asText()).isEqualTo("Jane");
  }
}
