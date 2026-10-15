package uk.gov.justice.laa.ia.datastore.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for the per-user API rate limit. */
@WithMockUser
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
@TestPropertySource(
    properties = {
      "laa.datastore.rate-limit.enabled=true",
      "laa.datastore.rate-limit.limit-for-period=2",
      "laa.datastore.rate-limit.refresh-period=1h"
    })
public class RateLimitIntegrationTest extends BaseIntegrationTest {

  @Test
  void whenLimitExceeded_thenReturnsTooManyRequests() throws Exception {
    mockMvc.perform(get("/api/v0/applications").withBearerReadToken()).andExpect(status().isOk());
    mockMvc.perform(get("/api/v0/applications").withBearerReadToken()).andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v0/applications").withBearerReadToken())
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "3600"));
  }
}
