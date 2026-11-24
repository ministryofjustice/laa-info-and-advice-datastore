package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity;
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
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE)));
    clearCache();
  }

  @Test
  void shouldGetApplicationsSuccessfully() throws Exception {
    setupApplications();

    resetQueryCount();
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
    assertThat(getQueryCount())
        .as("Expected only 1 query to be executed for the GET request")
        .isEqualTo(1);
  }

  @Test
  void shouldGetApplicationsSuccessfully_withOfficeIdFilter() throws Exception {
    // Arrange
    setupApplications();
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(SECONDARY_PROVIDER_OFFICE_CODE)));
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder ->
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
        ApplicationEntityGenerator.createWithoutId(builder ->
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
        ApplicationEntityGenerator.createWithoutId(builder ->
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
  void shouldIncludeExemptApplicationsAlongsideEligibleIndications() throws Exception {
    final var exempt = saveApplication(false, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    var eligible = saveApplication(true, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    saveIndication(eligible, true);
    var unknownEligible = saveApplication(null, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    saveIndication(unknownEligible, true);
    saveApplication(null, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    clearCache();

    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("eligibilityIndication", "eligible")
                .param("includeMeansExemptApplications", "true")
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(
            jsonPath(
                "$.content[*].id",
                containsInAnyOrder(
                    exempt.getId().toString(),
                    eligible.getId().toString(),
                    unknownEligible.getId().toString())));
  }

  @Test
  void shouldIncludeExemptApplicationsAlongsideIneligibleIndications() throws Exception {
    var exempt = saveApplication(false, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    saveIndication(exempt, true);
    var ineligible = saveApplication(true, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    saveIndication(ineligible, false);
    clearCache();

    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("eligibilityIndication", "ineligible")
                .param("includeMeansExemptApplications", "true")
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(
            jsonPath(
                "$.content[*].id",
                containsInAnyOrder(exempt.getId().toString(), ineligible.getId().toString())));
  }

  @Test
  void shouldPreserveEligibilityFilteringWhenExemptionsAreNotIncluded() throws Exception {
    saveApplication(false, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    var eligible = saveApplication(true, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    saveIndication(eligible, true);
    saveIndication(eligible, false);
    clearCache();

    for (var flag : List.of("", "false")) {
      var request =
          get("/api/v0/applications")
              .param("eligibilityIndication", "eligible")
              .withBearerReadToken();
      if (!flag.isEmpty()) {
        request.param("includeMeansExemptApplications", flag);
      }
      mockMvc
          .perform(request)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.content[0].id").value(eligible.getId().toString()))
          .andExpect(jsonPath("$.content[0].eligibilityIndication").value("ineligible"))
          .andExpect(jsonPath("$.content[0].meansAssessmentRequired").value(true));
    }
  }

  @Test
  void shouldIgnoreExemptionFlagWithoutIndication() throws Exception {
    var exempt = saveApplication(false, ApplicationState.DRAFT, PROVIDER_OFFICE_CODE);
    var unknown = saveApplication(null, ApplicationState.DRAFT, PROVIDER_OFFICE_CODE);
    clearCache();

    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("includeMeansExemptApplications", "true")
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(
            jsonPath(
                "$.content[*].id",
                containsInAnyOrder(exempt.getId().toString(), unknown.getId().toString())));
  }

  @Test
  void shouldApplyOtherFiltersAndPaginationWhenIncludingExemptions() throws Exception {
    final var exempt = saveApplication(false, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    var eligible = saveApplication(true, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    saveIndication(eligible, true);
    saveIndication(eligible, true);
    saveApplication(false, ApplicationState.DRAFT, PROVIDER_OFFICE_CODE);
    saveApplication(false, ApplicationState.COMPLETED, SECONDARY_PROVIDER_OFFICE_CODE);
    var unauthorizedOffice = UUID.randomUUID().toString();
    saveApplication(false, ApplicationState.COMPLETED, unauthorizedOffice);
    clearCache();

    var ids = new java.util.ArrayList<String>();
    for (var page : List.of("0", "1")) {
      var response =
          mockMvc
              .perform(
                  get("/api/v0/applications")
                      .param("eligibilityIndication", "eligible")
                      .param("includeMeansExemptApplications", "true")
                      .param("status", "COMPLETED")
                      .param("officeId", PROVIDER_OFFICE_CODE)
                      .param("page", page)
                      .param("size", "1")
                      .withBearerReadToken())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.totalElements").value(2))
              .andExpect(jsonPath("$.totalPages").value(2))
              .andExpect(jsonPath("$.content", hasSize(1)))
              .andReturn();
      ids.add(
          objectMapper
              .readTree(response.getResponse().getContentAsString())
              .get("content")
              .get(0)
              .get("id")
              .asText());
    }
    assertThat(ids)
        .containsExactlyInAnyOrder(exempt.getId().toString(), eligible.getId().toString());

    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("eligibilityIndication", "eligible")
                .param("includeMeansExemptApplications", "true")
                .param("officeId", unauthorizedOffice)
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void shouldReturnStoredMeansAssessmentRequirementInSummary() throws Exception {
    var exempt = saveApplication(false, ApplicationState.COMPLETED, PROVIDER_OFFICE_CODE);
    clearCache();

    mockMvc
        .perform(get("/api/v0/applications").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(exempt.getId().toString()))
        .andExpect(jsonPath("$.content[0].meansAssessmentRequired").value(false));
  }

  private ApplicationEntity saveApplication(
      Boolean meansAssessmentRequired, ApplicationState state, String officeCode) {
    return applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(officeCode)
                .applicationState(state)
                .meansAssessmentRequired(meansAssessmentRequired)));
  }

  private void saveIndication(ApplicationEntity application, Boolean indication) {
    entityManager
        .createNativeQuery(
            "UPDATE eligibility_assessments SET created_at = created_at - INTERVAL '1 second'"
                + " WHERE application_id = :applicationId")
        .setParameter("applicationId", application.getId())
        .executeUpdate();
    eligibilityAssessmentRepository.saveAndFlush(
        EligibilityAssessmentEntity.builder()
            .applicationId(application.getId())
            .resultJson(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode())
            .createdBy("test")
            .indication(indication)
            .build());
  }

  @Test
  void shouldFilterApplicationsByEligibilityIndication_eligible() throws Exception {
    // Arrange
    var eligibleApp =
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE));
    var ineligibleApp =
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE));

    var savedEligible = applicationRepository.save(eligibleApp);
    var savedIneligible = applicationRepository.save(ineligibleApp);

    // Create eligibility results with different indication values
    var eligibleResult =
        uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity.builder()
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
        uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity.builder()
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

    eligibilityAssessmentRepository.save(eligibleResult);
    eligibilityAssessmentRepository.save(ineligibleResult);
    clearCache();

    // Act & Assert - filter by eligible
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("eligibilityIndication", "eligible")
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
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE));
    var ineligibleApp =
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE));

    var savedEligible = applicationRepository.save(eligibleApp);
    var savedIneligible = applicationRepository.save(ineligibleApp);

    // Create eligibility results with different indication values
    var eligibleResult =
        uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity.builder()
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
        uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity.builder()
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

    eligibilityAssessmentRepository.save(eligibleResult);
    eligibilityAssessmentRepository.save(ineligibleResult);
    clearCache();

    // Act & Assert - filter by ineligible
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("eligibilityIndication", "ineligible")
                .withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].id").value(savedIneligible.getId().toString()))
        .andExpect(jsonPath("$.content[0].eligibilityIndication").value("ineligible"));
  }

  @Test
  void shouldExcludeDeletedApplicationsFromResults() throws Exception {
    // Arrange
    setupApplications();
    applicationRepository.save(
        ApplicationEntityGenerator.createWithoutId(builder ->
            builder
                .withDefaultClientDetails()
                .providerFirmCode(FIRM_CODE)
                .providerOfficeCode(PROVIDER_OFFICE_CODE)
                .deleted(true)));
    clearCache();

    // Act & Assert
    mockMvc
        .perform(get("/api/v0/applications").withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(DEFAULT_NUMBER_OF_APPLICATIONS)))
        .andExpect(jsonPath("$.totalElements").value(DEFAULT_NUMBER_OF_APPLICATIONS));
  }
}
