package uk.gov.justice.laa.ia.datastore.config;

import io.sentry.SentryLogEvent;
import io.sentry.SentryLogEventAttributeValue;
import io.sentry.SentryOptions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SentryPiiRedactor implements SentryOptions.Logs.BeforeSendLogCallback {

  private static final String REDACTED = "[REDACTED]";
  private static final Set<String> SENSITIVE_FIELDS =
      Set.of(
          "firstname",
          "lastname",
          "dateofbirth",
          "ninumber",
          "nationalinsurancenumber",
          "addressline1",
          "addressline2",
          "addressline3",
          "addressline4",
          "townorcity",
          "postcode",
          "county",
          "clientfirstname",
          "clientlastname");
  private static final Pattern LABELED_PII_PATTERN =
      Pattern.compile(
          "(?i)(?<![\\p{L}\\p{N}_])([\\\"']?(?:first[_-]?name|last[_-]?name|date[_-]?of[_-]?birth|ni[_-]?number|national[_-]?insurance[_-]?number|address[_-]?line[_-]?[1-4]|town[_-]?or[_-]?city|post[_-]?code|county|client[_-]?first[_-]?name|client[_-]?last[_-]?name)[\\\"']?\\s*[:=]\\s*)(?:\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|[^,;\\s}\\]]+)");

  @Override
  public SentryLogEvent execute(SentryLogEvent event) {
    Matcher matcher = LABELED_PII_PATTERN.matcher(event.getBody());
    String redactedBody = matcher.replaceAll("$1\"" + REDACTED + "\"");
    boolean containsPii = !redactedBody.equals(event.getBody());
    if (containsPii) {
      event.setBody(redactedBody);
    }

    Map<String, SentryLogEventAttributeValue> attributes = event.getAttributes();
    if (attributes != null) {
      Map<String, SentryLogEventAttributeValue> redactedAttributes =
          new LinkedHashMap<>(attributes.size());
      for (Map.Entry<String, SentryLogEventAttributeValue> attribute : attributes.entrySet()) {
        String key = attribute.getKey();
        SentryLogEventAttributeValue value = attribute.getValue();
        if (isSensitiveField(key)) {
          redactedAttributes.put(key, redactedValue());
          containsPii = true;
        } else if (value != null) {
          RedactedObject redacted = redactNestedValue(value.getValue());
          if (redacted.changed()) {
            redactedAttributes.put(
                key, new SentryLogEventAttributeValue(value.getType(), redacted.value()));
            containsPii = true;
          } else {
            redactedAttributes.put(key, value);
          }
        } else {
          redactedAttributes.put(key, null);
        }
      }

      if (containsPii) {
        redactedAttributes.replaceAll(
            (key, value) ->
                key.toLowerCase(Locale.ROOT).startsWith("message.parameter.")
                    ? redactedValue()
                    : value);
      }
      event.setAttributes(redactedAttributes);
    }

    redactUnknownFields(event);
    return event;
  }

  private static void redactUnknownFields(SentryLogEvent event) {
    Map<String, Object> unknown = event.getUnknown();
    if (unknown == null) {
      return;
    }

    RedactedObject redacted = redactNestedValue(unknown);
    if (redacted.changed()) {
      @SuppressWarnings("unchecked")
      Map<String, Object> redactedUnknown = (Map<String, Object>) redacted.value();
      event.setUnknown(redactedUnknown);
    }
  }

  private static RedactedObject redactNestedValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      Map<Object, Object> redacted = new LinkedHashMap<>(map.size());
      boolean changed = false;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        Object key = entry.getKey();
        if (key instanceof String fieldName && isSensitiveField(fieldName)) {
          redacted.put(key, REDACTED);
          changed = true;
        } else {
          RedactedObject nested = redactNestedValue(entry.getValue());
          redacted.put(key, nested.value());
          changed |= nested.changed();
        }
      }
      return new RedactedObject(changed ? redacted : value, changed);
    }

    if (value instanceof List<?> list) {
      List<Object> redacted = new ArrayList<>(list.size());
      boolean changed = false;
      for (Object item : list) {
        RedactedObject nested = redactNestedValue(item);
        redacted.add(nested.value());
        changed |= nested.changed();
      }
      return new RedactedObject(changed ? redacted : value, changed);
    }

    return new RedactedObject(value, false);
  }

  private static boolean isSensitiveField(String key) {
    return SENSITIVE_FIELDS.contains(key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""));
  }

  private static SentryLogEventAttributeValue redactedValue() {
    return new SentryLogEventAttributeValue("string", REDACTED);
  }

  private record RedactedObject(Object value, boolean changed) {}
}
