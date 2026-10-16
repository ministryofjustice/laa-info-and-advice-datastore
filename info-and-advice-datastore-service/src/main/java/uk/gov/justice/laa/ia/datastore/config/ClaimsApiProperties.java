package uk.gov.justice.laa.ia.datastore.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the claims API. The gateway is only the live implementation when {@code mode} is
 * {@code live}; otherwise a stub is used.
 *
 * @param mode {@code live} to call the claims API, {@code stub} to use canned data
 * @param baseUrl base URL of the claims API
 * @param apiKey API key, sent in the {@code Authorization} header
 * @param pageSize page size used when paging through retention lookup results
 * @param connectTimeout time allowed to establish a connection
 * @param readTimeout time allowed to wait for a response
 */
@ConfigurationProperties(prefix = "laa.datastore.claims-api")
public record ClaimsApiProperties(
    String mode,
    String baseUrl,
    String apiKey,
    int pageSize,
    Duration connectTimeout,
    Duration readTimeout) {
  public static final String LIVE_MODE = "live";
  public static final String STUB_MODE = "stub";
}
