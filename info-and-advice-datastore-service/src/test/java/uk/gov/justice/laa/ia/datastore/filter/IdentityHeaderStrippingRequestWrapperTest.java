package uk.gov.justice.laa.ia.datastore.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Unit tests for {@link IdentityHeaderStrippingRequestWrapper}. */
class IdentityHeaderStrippingRequestWrapperTest {

  @Test
  void shouldHideBlockedHeadersFromGetHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-User", "someone");
    request.addHeader("X-Remote-User", "someone");
    request.addHeader("X-User", "someone");
    request.addHeader("X-Auth-Request-Email", "someone@example.com");
    IdentityHeaderStrippingRequestWrapper wrapper =
        new IdentityHeaderStrippingRequestWrapper(request);

    assertThat(wrapper.getHeader("X-Forwarded-User")).isNull();
    assertThat(wrapper.getHeader("X-Remote-User")).isNull();
    assertThat(wrapper.getHeader("X-User")).isNull();
    assertThat(wrapper.getHeader("X-Auth-Request-Email")).isNull();
  }

  @Test
  void shouldHideBlockedHeadersCaseInsensitively() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("x-forwarded-user", "someone");
    IdentityHeaderStrippingRequestWrapper wrapper =
        new IdentityHeaderStrippingRequestWrapper(request);

    assertThat(wrapper.getHeader("X-FORWARDED-USER")).isNull();
    assertThat(Collections.list(wrapper.getHeaders("X-Forwarded-User"))).isEmpty();
  }

  @Test
  void shouldPassThroughOtherHeaders() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token");
    request.addHeader("X-Forwarded-User", "someone");
    IdentityHeaderStrippingRequestWrapper wrapper =
        new IdentityHeaderStrippingRequestWrapper(request);

    assertThat(wrapper.getHeader("Authorization")).isEqualTo("Bearer token");
    assertThat(Collections.list(wrapper.getHeaders("Authorization")))
        .containsExactly("Bearer token");
  }

  @Test
  void shouldExcludeBlockedHeadersFromHeaderNames() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token");
    request.addHeader("X-Forwarded-User", "someone");
    request.addHeader("X-Auth-Request-Email", "someone@example.com");
    IdentityHeaderStrippingRequestWrapper wrapper =
        new IdentityHeaderStrippingRequestWrapper(request);

    assertThat(Collections.list(wrapper.getHeaderNames()))
        .contains("Authorization")
        .doesNotContain("X-Forwarded-User", "X-Auth-Request-Email");
  }
}
