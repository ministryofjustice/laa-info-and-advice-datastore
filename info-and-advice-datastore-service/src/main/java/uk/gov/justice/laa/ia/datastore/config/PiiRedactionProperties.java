package uk.gov.justice.laa.ia.datastore.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for which JSON field names in event payloads are treated as PII.
 *
 * @param fields field names (matched at any depth in the payload) to redact
 */
@ConfigurationProperties(prefix = "laa.datastore.events.pii")
public record PiiRedactionProperties(List<String> fields) {}
