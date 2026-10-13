package uk.gov.justice.laa.ia.datastore.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * In-memory API rate limiting configuration. Limits are applied per caller, per container.
 *
 * @param enabled whether rate limiting is applied
 * @param limitForPeriod permitted requests per caller in each refresh period
 * @param refreshPeriod how often a caller's permits are replenished
 */
@ConfigurationProperties(prefix = "laa.datastore.rate-limit")
public record RateLimitProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("100") int limitForPeriod,
    @DefaultValue("1m") Duration refreshPeriod) {}
