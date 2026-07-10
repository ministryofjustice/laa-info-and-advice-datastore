package uk.gov.justice.laa.ia.datastore.utils.extensions;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.justice.laa.ia.datastore.utils.TestJwtConfig;

/**
 * Extension methods for MockHttpServletRequestBuilder to add custom headers and other
 * functionality.
 */
public class MockHttpServletRequestBuilderExtensions {
  /** Extension method to add a Bearer token to the headers. */
  public static MockHttpServletRequestBuilder withBearerToken(
      MockHttpServletRequestBuilder builder, String token) {
    return builder.header("Authorization", "Bearer " + token);
  }

  /** Extension method to add a Bearer token to the headers. */
  public static MockHttpServletRequestBuilder withBearerReadToken(
      MockHttpServletRequestBuilder builder) {
    return builder.header("Authorization", "Bearer " + TestJwtConfig.ACCESS_TOKEN);
  }

  /** Extension method to add a Bearer token to the headers. */
  public static MockHttpServletRequestBuilder withBearerWriteToken(
      MockHttpServletRequestBuilder builder) {
    return builder.header("Authorization", "Bearer " + TestJwtConfig.ACCESS_TOKEN);
  }
}
