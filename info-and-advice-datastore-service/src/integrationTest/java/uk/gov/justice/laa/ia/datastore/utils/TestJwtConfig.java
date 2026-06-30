package uk.gov.justice.laa.ia.datastore.utils;

import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import uk.gov.laa.springboot.oauth2.testsupport.StubJwtDecoder;
import uk.gov.laa.springboot.oauth2.testsupport.StubJwtToken;

/** Provides a stub {@link JwtDecoder} for integration tests, bypassing real JWT validation. */
@TestConfiguration
public class TestJwtConfig {

  public static final String READ_TOKEN = "applications-read-token";
  public static final String WRITE_TOKEN = "applications-write-token";

  /** Stub {@link JwtDecoder} seeded with tokens for each scope used in tests. */
  @Bean
  public JwtDecoder jwtDecoder() {
    return StubJwtDecoder.of(
        new StubJwtToken(
            READ_TOKEN, "test-user", null, new String[] {"applications:read"}, Map.of()),
        new StubJwtToken(
            WRITE_TOKEN, "test-user", null, new String[] {"applications:write"}, Map.of()));
  }
}
