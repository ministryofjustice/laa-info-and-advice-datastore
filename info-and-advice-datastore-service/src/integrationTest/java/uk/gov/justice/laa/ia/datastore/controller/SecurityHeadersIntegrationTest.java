package uk.gov.justice.laa.ia.datastore.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for CTRL-057 security response headers on the API. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class SecurityHeadersIntegrationTest extends BaseIntegrationTest {

  private static ResultMatcher[] securityHeaders() {
    return new ResultMatcher[] {
      header().string("X-Content-Type-Options", "nosniff"),
      header().string("Referrer-Policy", "no-referrer"),
      header().string("Cache-Control", "no-store"),
      header().string("Pragma", "no-cache"),
      header().exists("Strict-Transport-Security")
    };
  }

  @Test
  void whenAuthenticatedRequestSucceeds_thenSecurityHeadersPresent() throws Exception {
    mockMvc
        .perform(get("/api/v0/applications").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpectAll(securityHeaders());
  }

  @Test
  void whenRequestUnauthenticated_thenSecurityHeadersPresentOnError() throws Exception {
    mockMvc
        .perform(get("/api/v0/applications"))
        .andExpect(status().isUnauthorized())
        .andExpectAll(securityHeaders());
  }
}
