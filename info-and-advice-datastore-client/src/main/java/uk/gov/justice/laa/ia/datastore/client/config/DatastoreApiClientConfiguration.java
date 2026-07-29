package uk.gov.justice.laa.ia.datastore.client.config;

import java.io.IOException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.laa.ia.datastore.client.api.ApplicationApi;
import uk.gov.justice.laa.ia.datastore.client.invoker.ApiClient;

/**
 * Spring Boot autoconfiguration for the Info and Advice Datastore API client.
 *
 * <p>Requires:
 *
 * <ul>
 *   <li>An {@link OAuth2AuthorizedClientManager} bean (provided by spring-security-oauth2-client)
 *   <li>{@code laa.datastore.client.base-url} and {@code laa.datastore.client.client-registration-id} properties
 * </ul>
 *
 * <p>The configured {@link RestTemplate} attaches two auth headers to every request:
 *
 * <ul>
 *   <li>{@code Authorization}: app-level Bearer token acquired via the OAuth2 client credentials grant
 *   <li>{@code X-Authorization}: the incoming user's JWT, forwarded from the active {@link SecurityContextHolder}
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(DatastoreClientProperties.class)
public class DatastoreApiClientConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ApplicationApi applicationApi(
      DatastoreClientProperties props, OAuth2AuthorizedClientManager clientManager) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate
        .getInterceptors()
        .add(new DatastoreAuthInterceptor(clientManager, props.clientRegistrationId()));

    ApiClient apiClient = new ApiClient(restTemplate).setBasePath(props.baseUrl());
    return new ApplicationApi(apiClient);
  }

  /**
   * Intercepts outgoing requests to attach both required auth headers.
   *
   * <p>{@code Authorization} is acquired via the OAuth2 client credentials grant using the
   * configured registration ID. {@code X-Authorization} is the JWT of the currently authenticated
   * user, forwarded from the active security context so the datastore can extract the {@code
   * providerFirmId} claim.
   */
  private record DatastoreAuthInterceptor(
      OAuth2AuthorizedClientManager clientManager, String clientRegistrationId)
      implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
      attachAppToken(request);
      attachUserToken(request);
      return execution.execute(request, body);
    }

    private void attachAppToken(HttpRequest request) {
      OAuth2AuthorizeRequest authorizeRequest =
          OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
              .principal("datastore-client")
              .build();
      OAuth2AccessToken accessToken =
          clientManager.authorize(authorizeRequest).getAccessToken();
      request.getHeaders().setBearerAuth(accessToken.getTokenValue());
    }

    private void attachUserToken(HttpRequest request) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth instanceof AbstractOAuth2TokenAuthenticationToken<?> tokenAuth) {
        request
            .getHeaders()
            .set("X-Authorization", "Bearer " + tokenAuth.getToken().getTokenValue());
      }
    }
  }
}
