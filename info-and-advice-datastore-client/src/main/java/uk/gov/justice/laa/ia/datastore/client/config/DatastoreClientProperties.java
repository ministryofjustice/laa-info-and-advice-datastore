package uk.gov.justice.laa.ia.datastore.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties for the Info and Advice Datastore client. */
@ConfigurationProperties(prefix = "laa.datastore.client")
public record DatastoreClientProperties(
    /** Base URL of the datastore service, e.g. https://datastore.laa.gov.uk */
    String baseUrl,
    /**
     * Spring OAuth2 client registration ID to use for acquiring the app-level Authorization token.
     * Must match an entry under spring.security.oauth2.client.registration.*
     */
    String clientRegistrationId) {}
