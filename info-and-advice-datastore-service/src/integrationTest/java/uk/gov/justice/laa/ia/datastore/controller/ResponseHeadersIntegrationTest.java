package uk.gov.justice.laa.ia.datastore.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/**
 * Integration test for CTRL-057's {@code Referrer-Policy} header on the API. The other CTRL-057
 * headers (nosniff, no-store) come from Spring Security's defaults and HSTS from the ingress, so
 * are not asserted here.
 */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class ResponseHeadersIntegrationTest extends BaseIntegrationTest {

  @Test
  void whenAuthenticatedRequestSucceeds_thenReferrerPolicyPresent() throws Exception {
    mockMvc
        .perform(get("/api/v0/applications").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(header().string("Referrer-Policy", "no-referrer"));
  }

  @Test
  void whenRequestUnauthenticated_thenReferrerPolicyPresentOnError() throws Exception {
    mockMvc
        .perform(get("/api/v0/applications"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Referrer-Policy", "no-referrer"));
  }
}
