package uk.gov.justice.laa.ia.datastore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

/** Web security config. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** Configures the security behaviour. */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .permitAll())
        // this is the default CSRF configuration, but we're using it explicitly here so Snyk can
        // see it
        .csrf((csrf) -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
        .build();
  }
}
