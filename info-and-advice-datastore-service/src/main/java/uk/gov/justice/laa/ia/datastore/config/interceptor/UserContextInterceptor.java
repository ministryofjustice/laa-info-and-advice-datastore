package uk.gov.justice.laa.ia.datastore.config.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
  private static final String LAA_ACCOUNTS_CLAIM = "LAA_ACCOUNTS";

  private final UserContext userContext;
  private final TrustedCallerJwtDecoder trustedCallerJwtDecoder;
  private final ObjectMapper objectMapper;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws IOException {
    Jwt authenticatedJwt;
    try {
      authenticatedJwt = authenticateJwt(request);
    } catch (Exception e) {
      writeUnauthorized(response, "Invalid or missing authentication token");
      return false;
    }
    try {
      populateUserContext(authenticatedJwt);
      return true;
    } catch (IllegalArgumentException e) {
      writeUnauthorized(response, e.getMessage());
      return false;
    }
  }

  private void writeUnauthorized(HttpServletResponse response, String detail) throws IOException {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
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
    final String providerFirmCodeClaim = authenticatedJwt.getClaimAsString("FIRM_CODE");

    if (providerFirmCodeClaim == null || providerFirmCodeClaim.isEmpty()) {
      throw new IllegalArgumentException("Missing or invalid FIRM_CODE claim in JWT");
    }

    final List<String> officeCodesClaim = authenticatedJwt.getClaimAsStringList(LAA_ACCOUNTS_CLAIM);

    if (officeCodesClaim == null || officeCodesClaim.isEmpty()) {
      throw new IllegalArgumentException("Missing or invalid LAA_ACCOUNTS claim in JWT");
    }

    userContext.setProviderFirmCode(providerFirmCodeClaim);
    userContext.setOfficeCodes(officeCodesClaim);
  }
}
