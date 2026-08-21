package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
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
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)));
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
        .andExpect(jsonPath("$.content[0].id").isNotEmpty())
        .andExpect(jsonPath("$.content[0].referenceNumber").isNotEmpty())
        .andExpect(jsonPath("$.content[0].clientFirstName").isNotEmpty())
        .andExpect(jsonPath("$.content[0].clientLastName").isNotEmpty())
        .andExpect(jsonPath("$.content[0].modifiedAt").isNotEmpty())
        .andExpect(jsonPath("$.totalElements").value(DEFAULT_NUMBER_OF_APPLICATIONS))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.page").value(0));
  }

  @Test
  void shouldGetApplicationsSuccessfully_withOfficeIdFilter() throws Exception {
    // Arrange
    setupApplications();
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(SECONDARY_PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(SECONDARY_PROVIDER_OFFICE_CODE)));
    clearCache();
    // Act & Assert
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("officeId", SECONDARY_PROVIDER_OFFICE_CODE)
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].id").isNotEmpty())
        .andExpect(jsonPath("$.content[0].referenceNumber").isNotEmpty())
        .andExpect(jsonPath("$.totalElements").value(2));
  }

  @Test
  void shouldExcludeApplications_whenProviderOfficeCodeNotAuthorized() throws Exception {
    // Arrange
    setupApplications();
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(UUID.randomUUID().toString())));
    clearCache();

    // Act & Assert - the unauthorized application must not appear even though the
    // firm code matches
    mockMvc
        .perform(get("/api/v0/applications").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(DEFAULT_NUMBER_OF_APPLICATIONS)))
        .andExpect(jsonPath("$.totalElements").value(DEFAULT_NUMBER_OF_APPLICATIONS));
  }

  @Test
  void shouldGetApplicationsSuccessfully_withStatusFilter() throws Exception {
    // Arrange
    setupApplications();
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE)
                    .applicationState(ApplicationState.COMPLETED)));
    clearCache();
    // Act & Assert
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("status", ApplicationState.COMPLETED.getValue())
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").isNotEmpty())
        .andExpect(jsonPath("$.totalElements").value(1));
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

  @Test
  void shouldReturnOrderByLastModifiedDescending() throws Exception {
    // Arrange
    setupApplications();
    clearCache();

    // Act & Assert — page 0 of size 5 should return all items in descending order
    // of modifiedAt
    mockMvc
        .perform(
            get("/api/v0/applications").param("page", "0").param("size", "5").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content[*].modifiedAt", isDateDescendingOrder()));
  }

  @Test
  void isDateDescendingOrder_shouldReturnTrueForDescendingDates() {
    List<String> dates =
        List.of(
            "2023-01-01T12:00:00Z",
            "2022-12-31T12:00:00Z",
            "2022-12-30T12:00:00Z",
            "2022-12-29T12:00:00Z");
    final Matcher<? super List<String>> matcher = isDateDescendingOrder();
    assertThat(matcher.matches(dates)).isTrue();
    assertThat(matcher.matches(dates.reversed())).isFalse();
  }

  private Matcher<? super List<String>> isDateDescendingOrder() {
    return new TypeSafeMatcher<>() {
      @Override
      protected boolean matchesSafely(List<String> dates) {
        List<OffsetDateTime> parsedDates = dates.stream().map(OffsetDateTime::parse).toList();
        for (int i = 0; i < parsedDates.size() - 1; i++) {
          if (parsedDates.get(i).compareTo(parsedDates.get(i + 1)) < 0) {
            return false;
          }
        }
        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("list of dates either not parseable or not in descending order");
      }
    };
  }

  @Test
  void shouldFilterApplicationsByEligibilityIndication_eligible() throws Exception {
    // Arrange
    var eligibleApp =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE));
    var ineligibleApp =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE));

    var savedEligible = applicationRepository.save(eligibleApp);
    var savedIneligible = applicationRepository.save(ineligibleApp);

    // Create eligibility results with different indication values
    var eligibleResult =
        uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity.builder()
            .applicationId(savedEligible.getId())
            .resultJson(
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                    .objectNode()
                    .putObject("result_summary")
                    .putObject("overall_result")
                    .put("result", "eligible"))
            .createdBy("test")
            .indication(true)
            .build();

    var ineligibleResult =
        uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity.builder()
            .applicationId(savedIneligible.getId())
            .resultJson(
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                    .objectNode()
                    .putObject("result_summary")
                    .putObject("overall_result")
                    .put("result", "ineligible"))
            .createdBy("test")
            .indication(false)
            .build();

    eligibilityResultRepository.save(eligibleResult);
    eligibilityResultRepository.save(ineligibleResult);
    clearCache();

    // Act & Assert - filter by eligible
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("eligibilityIndication", "ELIGIBLE")
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(savedEligible.getId().toString()))
        .andExpect(jsonPath("$.content[0].eligibilityIndication").value("eligible"));
  }

  @Test
  void shouldFilterApplicationsByEligibilityIndication_ineligible() throws Exception {
    // Arrange
    var eligibleApp =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE));
    var ineligibleApp =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(FIRM_CODE)
                    .providerOfficeCode(PROVIDER_OFFICE_CODE));

    var savedEligible = applicationRepository.save(eligibleApp);
    var savedIneligible = applicationRepository.save(ineligibleApp);

    // Create eligibility results with different indication values
    var eligibleResult =
        uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity.builder()
            .applicationId(savedEligible.getId())
            .resultJson(
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                    .objectNode()
                    .putObject("result_summary")
                    .putObject("overall_result")
                    .put("result", "eligible"))
            .createdBy("test")
            .indication(true)
            .build();

    var ineligibleResult =
        uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity.builder()
            .applicationId(savedIneligible.getId())
            .resultJson(
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
                    .objectNode()
                    .putObject("result_summary")
                    .putObject("overall_result")
                    .put("result", "ineligible"))
            .createdBy("test")
            .indication(false)
            .build();

    eligibilityResultRepository.save(eligibleResult);
    eligibilityResultRepository.save(ineligibleResult);
    clearCache();

    // Act & Assert - filter by ineligible
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("eligibilityIndication", "INELIGIBLE")
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(savedIneligible.getId().toString()))
        .andExpect(jsonPath("$.content[0].eligibilityIndication").value("ineligible"));
  }
}
