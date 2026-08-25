package uk.gov.justice.laa.ia.datastore.config.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.justice.laa.ia.datastore.config.TrustedCallerJwtDecoder;
import uk.gov.justice.laa.ia.datastore.context.UserContext;

/** Tests to ensure the UserContext is populated from the interceptor ok. */
@ExtendWith(MockitoExtension.class)
public class UserContextInterceptorTests {

  private final UserContext userContext = new UserContext();
  private final TrustedCallerJwtDecoder mockTrustedCallerJwtDecoder =
      mock(TrustedCallerJwtDecoder.class);
  private final Jwt mockJwt = mock(Jwt.class);
  private final UserContextInterceptor interceptor =
      new UserContextInterceptor(userContext, mockTrustedCallerJwtDecoder, new ObjectMapper());
  private final MockHttpServletRequest request;
  private final MockHttpServletResponse response;

  UserContextInterceptorTests() {
    when(mockTrustedCallerJwtDecoder.decode(any())).thenReturn(mockJwt);
    request = new MockHttpServletRequest();
    request.addHeader("X-Authorization", "Bearer valid.jwt.token");
    response = new MockHttpServletResponse();
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  private void givenAuthenticatedPrimaryJwt(String oid, String tid) {
    Jwt primaryJwt = mock(Jwt.class);
    lenient().when(primaryJwt.getClaimAsString("oid")).thenReturn(oid);
    lenient().when(primaryJwt.getClaimAsString("tid")).thenReturn(tid);
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(primaryJwt));
  }

  @Test
  void preHandle_shouldAuthenticate_xAuthorizatonHeader() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt("user-oid", "tenant-tid");
    when(mockJwt.getClaimAsString("FIRM_CODE")).thenReturn("123456");
    when(mockJwt.getClaimAsStringList("LAA_ACCOUNTS")).thenReturn(List.of("office1"));
    when(mockJwt.getClaimAsString("oid")).thenReturn("user-oid");
    when(mockJwt.getClaimAsString("tid")).thenReturn("tenant-tid");

    // Act & Assert
    assertTrue(interceptor.preHandle(request, response, null));
  }

  @Test
  void preHandle_shouldPopulateUserContext_whenValidJwt() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt("user-oid", "tenant-tid");
    String expectedProviderFirmCode = "123456";
    List<String> expectedOfficeCodes = List.of("office1", "office2");
    when(mockJwt.getClaimAsString("FIRM_CODE")).thenReturn(expectedProviderFirmCode);
    when(mockJwt.getClaimAsStringList("LAA_ACCOUNTS")).thenReturn(expectedOfficeCodes);
    when(mockJwt.getClaimAsString("oid")).thenReturn("user-oid");
    when(mockJwt.getClaimAsString("tid")).thenReturn("tenant-tid");

    // Act & Assert
    assertTrue(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(userContext.getProviderFirmCode()).isEqualTo(expectedProviderFirmCode);
    assertThat(userContext.getOfficeCodes()).isEqualTo(expectedOfficeCodes);
  }

  @Test
  void preHandle_shouldReturnUnauthorized_whenAuthenticationFails() throws Exception {
    // Arrange
    when(mockTrustedCallerJwtDecoder.decode(any()))
        .thenThrow(new BadJwtException("Authentication failed"));

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("Invalid or missing authentication token");
  }

  @Test
  void preHandle_whenMissingProviderFirmCodeClaim_shouldReturnUnauthorized() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt("user-oid", "tenant-tid");
    when(mockJwt.getClaimAsString("FIRM_CODE")).thenReturn(null);
    when(mockJwt.getClaimAsString("oid")).thenReturn("user-oid");
    when(mockJwt.getClaimAsString("tid")).thenReturn("tenant-tid");

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("Missing or invalid FIRM_CODE claim in JWT");
  }

  @Test
  void preHandle_whenMissingOfficeCodesClaim_shouldReturnUnauthorized() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt("user-oid", "tenant-tid");
    when(mockJwt.getClaimAsString("FIRM_CODE")).thenReturn("123456");
    when(mockJwt.getClaimAsStringList("LAA_ACCOUNTS")).thenReturn(null);
    when(mockJwt.getClaimAsString("oid")).thenReturn("user-oid");
    when(mockJwt.getClaimAsString("tid")).thenReturn("tenant-tid");

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentAsString())
        .contains("Missing or invalid LAA_ACCOUNTS claim in JWT");
  }

  @Test
  void preHandle_whenOidDiffersFromPrimaryToken_shouldReturnUnauthorized() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt("user-oid", "tenant-tid");
    when(mockJwt.getClaimAsString("oid")).thenReturn("different-oid");
    when(mockJwt.getClaimAsString("tid")).thenReturn("tenant-tid");

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString())
        .contains("Forwarded token identity does not match caller identity");
  }

  @Test
  void preHandle_whenTidDiffersFromPrimaryToken_shouldReturnUnauthorized() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt("user-oid", "tenant-tid");
    when(mockJwt.getClaimAsString("oid")).thenReturn("user-oid");
    when(mockJwt.getClaimAsString("tid")).thenReturn("different-tid");

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString())
        .contains("Forwarded token identity does not match caller identity");
  }

  @Test
  void preHandle_whenPrimaryTokenMissingOid_shouldReturnUnauthorized() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt(null, "tenant-tid");
    when(mockJwt.getClaimAsString("oid")).thenReturn("user-oid");
    when(mockJwt.getClaimAsString("tid")).thenReturn("tenant-tid");

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("Missing oid or tid claim");
  }

  @Test
  void preHandle_whenForwardedTokenMissingOid_shouldReturnUnauthorized() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt("user-oid", "tenant-tid");
    when(mockJwt.getClaimAsString("oid")).thenReturn(null);
    when(mockJwt.getClaimAsString("tid")).thenReturn("tenant-tid");

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("Missing oid or tid claim");
  }

  @Test
  void preHandle_shouldSetCorrelationIdOnUserContext_fromMdc() throws Exception {
    // Arrange
    givenAuthenticatedPrimaryJwt("user-oid", "tenant-tid");
    when(mockJwt.getClaimAsString("FIRM_CODE")).thenReturn("123456");
    when(mockJwt.getClaimAsStringList("LAA_ACCOUNTS")).thenReturn(List.of("office1"));
    when(mockJwt.getClaimAsString("oid")).thenReturn("user-oid");
    when(mockJwt.getClaimAsString("tid")).thenReturn("tenant-tid");
    MDC.put("correlationId", "test-correlation-id");

    // Act
    assertTrue(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(userContext.getCorrelationId()).isEqualTo("test-correlation-id");
  }
}
