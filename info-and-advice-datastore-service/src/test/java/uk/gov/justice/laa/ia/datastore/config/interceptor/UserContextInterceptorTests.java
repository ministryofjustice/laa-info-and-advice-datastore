package uk.gov.justice.laa.ia.datastore.config.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.justice.laa.ia.datastore.context.UserContext;

/** Tests to ensure the UserContext is populated from the interceptor ok. */
@ExtendWith(MockitoExtension.class)
public class UserContextInterceptorTests {

  private final UserContext userContext = new UserContext();
  private final AuthenticationManagerResolver<HttpServletRequest> mockAuthMgrResolver =
      mock(AuthenticationManagerResolver.class);
  private final AuthenticationManager mockAuthMgr = mock(AuthenticationManager.class);
  private final Authentication mockAuthentication = mock(Authentication.class);
  private final Jwt mockJwt = mock(Jwt.class);
  private final UserContextInterceptor interceptor =
      new UserContextInterceptor(userContext, mockAuthMgrResolver);
  private final MockHttpServletRequest request;
  private final MockHttpServletResponse response;

  UserContextInterceptorTests() {
    when(mockAuthMgrResolver.resolve(any(HttpServletRequest.class))).thenReturn(mockAuthMgr);
    when(mockAuthMgr.authenticate(any())).thenReturn(mockAuthentication);
    when(mockAuthentication.getPrincipal()).thenReturn(mockJwt);
    request = new MockHttpServletRequest();
    request.addHeader("X-Authorization", "Bearer valid.jwt.token");
    response = new MockHttpServletResponse();
  }

  @Test
  void preHandle_shouldAuthenticate_xAuthorizatonHeader() throws Exception {
    // Arrange
    when(mockJwt.getClaimAsString("providerFirmId")).thenReturn(UUID.randomUUID().toString());

    // Act & Assert
    assertTrue(interceptor.preHandle(request, response, null));
  }

  @Test
  void preHandle_shouldPopulateUserContext_whenValidJwt() throws Exception {
    // Arrange
    UUID expectedProviderFirmId = UUID.randomUUID();
    when(mockJwt.getClaimAsString("providerFirmId")).thenReturn(expectedProviderFirmId.toString());

    // Act & Assert
    assertTrue(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(userContext.getProviderFirmId()).isEqualTo(expectedProviderFirmId);
  }

  @Test
  void preHandle_shouldReturnUnauthorized_whenAuthenticationFails() throws Exception {
    // Arrange
    when(mockAuthMgr.authenticate(any()))
        .thenThrow(new AuthenticationException("Authentication failed") {});

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  void preHandle_whenMissingProviderFirmIdClaim_shouldReturnUnauthorized() throws Exception {
    // Arrange
    when(mockJwt.getClaimAsString("providerFirmId")).thenReturn(null);

    // Act & Assert
    assertFalse(interceptor.preHandle(request, response, null));

    // Assert
    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(response.getContentAsString())
        .contains("Missing or invalid format for providerFirmId claim in JWT");
  }
}
