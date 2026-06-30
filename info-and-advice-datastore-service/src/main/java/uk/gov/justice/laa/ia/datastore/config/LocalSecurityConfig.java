package uk.gov.justice.laa.ia.datastore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Overrides the OAuth2 security filter chain for local development so the app can run without a
 * real auth server.
 *
 * <p>TODO: Remove this class once a real OAuth2 issuer URI and audience are configured and
 * available locally.
 */
@Configuration
@Profile("local")
public class LocalSecurityConfig {

  /** Permits all requests for local development without a real auth server. */
  @Bean("oauth2SecurityFilterChain")
  public SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }
}
