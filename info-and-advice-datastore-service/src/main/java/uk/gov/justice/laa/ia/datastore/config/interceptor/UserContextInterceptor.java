package uk.gov.justice.laa.ia.datastore.config.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.laa.ia.datastore.context.UserContext;

/** Interceptor that will provide data for the UserContext request scope. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

  private final UserContext userContext;
  private ObjectMapper objectMapper = new ObjectMapper();
  private static final String AUTHORIZATION_HEADER = "Authorization";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
    String[] chunks = authorizationHeader.split("\\.");
    String payload = new String(Base64.getDecoder().decode(chunks[1]));
    JsonNode payloadNode = objectMapper.readTree(payload);
    UUID providerFirmId = UUID.fromString(payloadNode.get("providerFirmId").asText());
    userContext.setProviderFirmId(providerFirmId);
    return true; // Continue with the next interceptor or the handler itself
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView)
      throws Exception {
    // Implement logic to clean up user context after handling the request
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    // Implement logic to perform any final cleanup after the request has been completed
  }
}
