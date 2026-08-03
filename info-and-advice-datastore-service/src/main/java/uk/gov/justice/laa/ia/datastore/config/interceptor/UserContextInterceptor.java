package uk.gov.justice.laa.ia.datastore.config.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.justice.laa.ia.datastore.config.TrustedCallerJwtDecoder;
import uk.gov.justice.laa.ia.datastore.context.UserContext;

/** Interceptor that will provide data for the UserContext request scope. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {
  private static final String AUTHORIZATION_HEADER = "X-Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final UserContext userContext;
  private final TrustedCallerJwtDecoder trustedCallerJwtDecoder;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    Jwt authenticatedJwt;
    try {
      authenticatedJwt = authenticateJwt(request);
    } catch (Exception e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return false;
    }
    try {
      populateUserContext(authenticatedJwt);
      return true;
    } catch (IllegalArgumentException e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("Missing or invalid FIRM_CODE claim in JWT");
      return false;
    }
  }

  private String extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
      throw new IllegalArgumentException("Missing or invalid X-Authorization header");
    }
    return authorizationHeader.substring(BEARER_PREFIX.length());
  }

  private Jwt authenticateJwt(HttpServletRequest request) {
    final String jwt = extractBearerToken(request.getHeader(AUTHORIZATION_HEADER));
    return trustedCallerJwtDecoder.decode(jwt);
  }

  private void populateUserContext(Jwt authenticatedJwt) {
    final Object providerFirmCodeClaim = authenticatedJwt.getClaim("FIRM_CODE");

    if (providerFirmCodeClaim == null || providerFirmCodeClaim.toString().isEmpty()) {
      throw new IllegalArgumentException("Missing FIRM_CODE claim in JWT");
    }

    userContext.setProviderFirmCode(providerFirmCodeClaim.toString());
  }
}
