package uk.gov.justice.laa.ia.datastore.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.sentry.SentryId;
import io.sentry.SentryLogEvent;
import io.sentry.SentryLogEventAttributeValue;
import io.sentry.SentryLogLevel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SentryPiiRedactorTest {

  private static final String REDACTED = "[REDACTED]";
  private static final List<String> SENSITIVE_FIELDS =
      List.of(
          "firstName",
          "lastName",
          "dateOfBirth",
          "niNumber",
          "nationalInsuranceNumber",
          "addressLine1",
          "addressLine2",
          "addressLine3",
          "addressLine4",
          "townOrCity",
          "postCode",
          "county",
          "clientFirstName",
          "clientLastName");

  private final SentryPiiRedactor sut = new SentryPiiRedactor();

  @Test
  void redactsSensitiveAttributesAndMessageParameters() {
    Map<String, SentryLogEventAttributeValue> attributes = new LinkedHashMap<>();
    for (String field : SENSITIVE_FIELDS) {
      attributes.put(field, new SentryLogEventAttributeValue("string", "pii-" + field));
    }
    attributes.put("message.parameter.0", new SentryLogEventAttributeValue("string", "pii-value"));
    attributes.put("request.id", new SentryLogEventAttributeValue("string", "request-123"));
    attributes.put(
        "client",
        new SentryLogEventAttributeValue(
            "object", Map.of("address", Map.of("post_code", "pii-postcode"))));
    SentryLogEvent event = logEvent("Client details saved", attributes);

    assertThat(sut.execute(event)).isSameAs(event);

    Map<String, SentryLogEventAttributeValue> redacted = event.getAttributes();
    for (String field : SENSITIVE_FIELDS) {
      assertThat(redacted.get(field).getValue()).isEqualTo(REDACTED);
    }
    assertThat(redacted.get("message.parameter.0").getValue()).isEqualTo(REDACTED);
    assertThat(redacted.get("request.id").getValue()).isEqualTo("request-123");
    assertThat(redacted.get("client").getValue())
        .isEqualTo(Map.of("address", Map.of("post_code", REDACTED)));
  }

  @Test
  void redactsSensitiveFieldsInLogBody() {
    String body =
        SENSITIVE_FIELDS.stream()
            .map(field -> "\"" + field + "\":\"pii-" + field + "\"")
            .reduce((left, right) -> left + ", " + right)
            .orElseThrow();
    SentryLogEvent event = logEvent("{" + body + "}", Map.of("request.id", attribute("123")));

    sut.execute(event);

    assertThat(event.getBody()).doesNotContain("pii-");
    for (String field : SENSITIVE_FIELDS) {
      assertThat(event.getBody()).contains("\"" + field + "\":\"" + REDACTED + "\"");
    }
  }

  @Test
  void matchesCaseAndSeparatorVariantsOfSensitiveAttributeNames() {
    Map<String, SentryLogEventAttributeValue> attributes =
        Map.of("NATIONAL_INSURANCE_NUMBER", attribute("pii-number"));
    SentryLogEvent event = logEvent("A log message", attributes);

    sut.execute(event);

    assertThat(event.getAttributes().get("NATIONAL_INSURANCE_NUMBER").getValue())
        .isEqualTo(REDACTED);
  }

  private static SentryLogEvent logEvent(
      String body, Map<String, SentryLogEventAttributeValue> attributes) {
    SentryLogEvent event = new SentryLogEvent(new SentryId(), 0.0, body, SentryLogLevel.INFO);
    event.setAttributes(new LinkedHashMap<>(attributes));
    return event;
  }

  private static SentryLogEventAttributeValue attribute(String value) {
    return new SentryLogEventAttributeValue("string", value);
  }
}
