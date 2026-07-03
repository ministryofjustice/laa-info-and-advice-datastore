package uk.gov.justice.laa.ia.datastore.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for getting applications. */
@WithMockUser
@ExtensionMethod({
  ApplicationEntityBuilderExtensions.class,
  MockHttpServletRequestBuilderExtensions.class
})
public class GetApplicationsIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;
  private static final int DEFAULT_NUMBER_OF_APPLICATIONS = 5;

  void setupApplications() {
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder -> builder.withDefaultClientDetails()));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder -> builder.withDefaultClientDetails()));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder -> builder.withDefaultClientDetails()));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder -> builder.withDefaultClientDetails()));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder -> builder.withDefaultClientDetails()));
    clearCache();
  }

  @Test
  void shouldGetApplicationsSuccessfully() throws Exception {
    setupApplications();
    // Act & Assert
    mockMvc
        .perform(get("/api/v0/applications").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.*", hasSize(DEFAULT_NUMBER_OF_APPLICATIONS)))
        .andExpect(jsonPath("$[0].providerOfficeId").isNotEmpty());
  }

  @Test
  void shouldGetApplicationsSuccessfully_withOfficeIdFilter() throws Exception {
    // Arrange
    setupApplications();
    final UUID officeId = UUID.randomUUID();
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeId(officeId)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeId(officeId)));
    clearCache();
    // Act & Assert
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("officeId", officeId.toString())
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.*", hasSize(2)))
        .andExpect(jsonPath("$[0].providerOfficeId").value(officeId.toString()));
  }
}
