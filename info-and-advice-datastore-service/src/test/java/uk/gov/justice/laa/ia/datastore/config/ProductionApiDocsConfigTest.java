package uk.gov.justice.laa.ia.datastore.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/** CTRL-058: Swagger UI and API docs must be disabled in production. */
class ProductionApiDocsConfigTest {

  private static PropertySource<?> load(String file) throws IOException {
    List<PropertySource<?>> sources =
        new YamlPropertySourceLoader().load(file, new ClassPathResource(file));
    return sources.get(0);
  }

  @Test
  void prodProfileDisablesSwaggerUiAndApiDocs() throws IOException {
    PropertySource<?> prod = load("application-prod.yml");

    assertThat(prod.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(false);
    assertThat(prod.getProperty("springdoc.api-docs.enabled")).isEqualTo(false);
  }

  @Test
  void prodProfileDoesNotExposeDocsAsUnprotectedUris() throws IOException {
    PropertySource<?> prod = load("application-prod.yml");

    for (int i = 0;
        prod.containsProperty("laa.springboot.starter.oauth2.unprotected-uris[" + i + "]");
        i++) {
      assertThat(
              (String)
                  prod.getProperty("laa.springboot.starter.oauth2.unprotected-uris[" + i + "]"))
          .doesNotContain("swagger")
          .doesNotContain("api-docs");
    }
  }

  @Test
  void defaultProfileDoesNotDisableDocsSoStagingAndUatKeepThem() throws IOException {
    PropertySource<?> base = load("application.yml");

    assertThat(base.getProperty("springdoc.swagger-ui.enabled")).isNull();
    assertThat(base.getProperty("springdoc.api-docs.enabled")).isNull();
  }
}
