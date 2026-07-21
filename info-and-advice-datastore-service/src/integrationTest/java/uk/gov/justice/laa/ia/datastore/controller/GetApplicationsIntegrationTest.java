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
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerFirmId(PROVIDER_FIRM_ID)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerFirmId(PROVIDER_FIRM_ID)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerFirmId(PROVIDER_FIRM_ID)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerFirmId(PROVIDER_FIRM_ID)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerFirmId(PROVIDER_FIRM_ID)));
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
        .andExpect(jsonPath("$.content", hasSize(DEFAULT_NUMBER_OF_APPLICATIONS)))
        .andExpect(jsonPath("$.content[0].providerOfficeId").isNotEmpty())
        .andExpect(jsonPath("$.totalElements").value(DEFAULT_NUMBER_OF_APPLICATIONS))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.page").value(0));
  }

  @Test
  void shouldGetApplicationsSuccessfully_withOfficeIdFilter() throws Exception {
    // Arrange
    setupApplications();
    final UUID officeId = UUID.randomUUID();
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmId(PROVIDER_FIRM_ID)
                    .providerOfficeId(officeId)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmId(PROVIDER_FIRM_ID)
                    .providerOfficeId(officeId)));
    clearCache();
    // Act & Assert
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("officeId", officeId.toString())
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].providerOfficeId").value(officeId.toString()))
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldRespectPageAndSizeParams() throws Exception {
    // Arrange
    setupApplications();
    clearCache();
    // Act & Assert
    mockMvc
        .perform(
            get("/api/v0/applications").param("page", "0").param("size", "2").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.totalElements").value(DEFAULT_NUMBER_OF_APPLICATIONS))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.page").value(0));
  }

  @Test
  void shouldReturnCorrectItemsOnSubsequentPage() throws Exception {
    // Arrange
    setupApplications();
    clearCache();

    // Act & Assert — page 1 of size 2 should return items 3-4
    mockMvc
        .perform(
            get("/api/v0/applications").param("page", "1").param("size", "2").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.totalElements").value(DEFAULT_NUMBER_OF_APPLICATIONS))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.page").value(1));
  }

  @Test
  void shouldReturnRemainingItemsOnLastPage() throws Exception {
    // Arrange
    setupApplications();
    clearCache();

    // Act & Assert — page 2 of size 2 should return the single remaining item
    mockMvc
        .perform(
            get("/api/v0/applications").param("page", "2").param("size", "2").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.totalElements").value(DEFAULT_NUMBER_OF_APPLICATIONS))
        .andExpect(jsonPath("$.totalPages").value(3))
        .andExpect(jsonPath("$.page").value(2));
  }

  @Test
  void shouldReturnEmptyContentBeyondLastPage() throws Exception {
    // Arrange
    setupApplications();
    clearCache();

    // Act & Assert — page 99 is beyond all results
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("page", "99")
                .param("size", "2")
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(0)))
        .andExpect(jsonPath("$.totalElements").value(DEFAULT_NUMBER_OF_APPLICATIONS))
        .andExpect(jsonPath("$.page").value(99));
  }
}
