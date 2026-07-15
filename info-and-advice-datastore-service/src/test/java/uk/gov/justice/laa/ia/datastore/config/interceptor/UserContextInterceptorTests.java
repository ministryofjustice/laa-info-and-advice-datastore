package uk.gov.justice.laa.ia.datastore.config.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.justice.laa.ia.datastore.context.UserContext;

/** Tests to ensure the UserContext is populated from the interceptor ok. */
public class UserContextInterceptorTests {
  private UserContextInterceptor sut;
  private UserContext userContext;

  @BeforeEach
  void setUp() {
    userContext = new UserContext();
    sut = new UserContextInterceptor(userContext);
  }

  @Test
  void preHandle_shouldSetProviderFirmIdInUserContext() throws Exception {
    // Arrange
    // Create a mock HttpServletRequest with a valid JWT token in the Authorization header
    UUID providerFirmId = UUID.randomUUID();
    MockHttpServletRequestBuilder builder =
        post("/api/v0/applications:start-case").header("Authorization", "");
    var request = builder.buildRequest(null);

    // Act
    var result = sut.preHandle(request, null, null);

    // Assert
    assertTrue(result);
    assertThat(userContext.getProviderFirmId()).isEqualTo(providerFirmId);
  }

  @Test
  void preHandle_givenProviderFirmId_shouldReturnFalse() throws Exception {
    // Arrange
    // Create a mock HttpServletRequest with a valid JWT token in the Authorization header
    MockHttpServletRequestBuilder builder =
        post("/api/v0/applications:start-case").header("Authorization", "");
    var request = builder.buildRequest(null);

    // Act
    // Call the preHandle method of the interceptor
    var result = sut.preHandle(request, null, null);

    // Assert
    assertFalse(result);
  }
}
