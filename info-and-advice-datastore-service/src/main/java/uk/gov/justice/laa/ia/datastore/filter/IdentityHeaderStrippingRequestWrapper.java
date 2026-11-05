package uk.gov.justice.laa.ia.datastore.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Hides forwarded identity headers from downstream code. */
class IdentityHeaderStrippingRequestWrapper extends HttpServletRequestWrapper {

  private static final Set<String> BLOCKED_HEADERS =
      Set.of("x-forwarded-user", "x-remote-user", "x-user");

  private static final String BLOCKED_HEADER_PREFIX = "x-auth-request-";

  IdentityHeaderStrippingRequestWrapper(HttpServletRequest request) {
    super(request);
  }

  @Override
  public String getHeader(String name) {
    return isBlocked(name) ? null : super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {
    return isBlocked(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    List<String> names = Collections.list(super.getHeaderNames());
    names.removeIf(this::isBlocked);
    return Collections.enumeration(names);
  }

  private boolean isBlocked(String name) {
    if (name == null) {
      return false;
    }
    String lower = name.toLowerCase(Locale.ROOT);
    return BLOCKED_HEADERS.contains(lower) || lower.startsWith(BLOCKED_HEADER_PREFIX);
  }
}
