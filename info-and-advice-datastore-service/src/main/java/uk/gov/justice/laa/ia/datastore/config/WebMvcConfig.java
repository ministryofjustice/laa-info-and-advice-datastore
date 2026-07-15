package uk.gov.justice.laa.ia.datastore.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.justice.laa.ia.datastore.config.interceptor.UserContextInterceptor;

/** Web MVC configuration to register interceptors. */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

  private final UserContextInterceptor userContextInterceptor;

  @Override
  public void addInterceptors(
      org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
    registry.addInterceptor(userContextInterceptor).addPathPatterns("/api/**");
    ;
  }
}
