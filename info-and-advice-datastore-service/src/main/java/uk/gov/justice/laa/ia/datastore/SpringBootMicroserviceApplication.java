package uk.gov.justice.laa.ia.datastore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import uk.gov.justice.laa.ia.datastore.config.PiiRedactionProperties;

/** Entry point for the Spring Boot microservice application. */
@SpringBootApplication
@EnableConfigurationProperties(PiiRedactionProperties.class)
public class SpringBootMicroserviceApplication {

  /**
   * The application main method.
   *
   * @param args the application arguments.
   */
  public static void main(String[] args) {
    SpringApplication.run(SpringBootMicroserviceApplication.class, args);
  }
}
