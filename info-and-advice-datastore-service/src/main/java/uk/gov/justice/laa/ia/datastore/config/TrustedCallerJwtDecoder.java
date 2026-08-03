package uk.gov.justice.laa.ia.datastore.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Decodes and validates the {@code X-Authorization} token forwarded by a trusted caller (e.g. RCW
 * API). Deliberately not a {@link JwtDecoder} bean itself - it must stay isolated from the primary
 * resource-server tenants so this trust never extends to the main {@code Authorization}
 * header/endpoint authorization, and registering a {@link JwtDecoder} bean here would also
 * short-circuit the starter's {@code @ConditionalOnMissingBean(JwtDecoder.class)} multi-tenant
 * resolver.
 */
@Component
public class TrustedCallerJwtDecoder {

  private final String issuerUri;
  private final String audience;
  private volatile JwtDecoder delegate;

  /** Captures the trusted caller's issuer/audience config; the decoder itself is built lazily. */
  public TrustedCallerJwtDecoder(
      @Value("${laa.datastore.trusted-caller.issuer-uri}") String issuerUri,
      @Value("${laa.datastore.trusted-caller.audience}") String audience) {
    this.issuerUri = issuerUri;
    this.audience = audience;
  }

  public Jwt decode(String token) {
    return delegate().decode(token);
  }

  // lazy: avoids an OIDC discovery network call during application startup/bean creation
  private JwtDecoder delegate() {
    JwtDecoder decoder = delegate;
    if (decoder == null) {
      synchronized (this) {
        decoder = delegate;
        if (decoder == null) {
          decoder = buildDecoder();
          delegate = decoder;
        }
      }
    }
    return decoder;
  }

  private JwtDecoder buildDecoder() {
    NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator()));
    return decoder;
  }

  private OAuth2TokenValidator<Jwt> audienceValidator() {
    return jwt -> {
      List<String> tokenAudiences = jwt.getAudience();
      if (tokenAudiences != null && tokenAudiences.contains(audience)) {
        return OAuth2TokenValidatorResult.success();
      }
      return OAuth2TokenValidatorResult.failure(
          new OAuth2Error("invalid_token", "JWT audience is not trusted", null));
    };
  }
}
