package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for getting an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class GetApplicationIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldGetApplicationSuccessfully() throws Exception {
    // Arrange
    ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder
                  .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                  .referenceNumber("L-ABC-123")
                  .providerFirmCode(FIRM_CODE);
            });
    ApplicationEntity savedEntity = applicationRepository.save(entity);

    clearCache();
    ApplicationEntity refreshedEntity =
        applicationRepository.findById(savedEntity.getId()).orElseThrow();

    // Act & Assert
    mockMvc
        .perform(get("/api/v0/applications/{id}", savedEntity.getId()).withBearerReadToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(savedEntity.getId().toString()))
        .andExpect(
            jsonPath("$.individualLegalAidNumber")
                .value(savedEntity.getClientDetails().getId().toString()))
        .andExpect(jsonPath("$.applicationType").value(savedEntity.getApplicationType()))
        .andExpect(
            jsonPath("$.providerOfficeId").value(savedEntity.getProviderOfficeId().toString()))
        .andExpect(jsonPath("$.referenceNumber").value(refreshedEntity.getReferenceNumber()));
  }

  @Test
  void shouldGetApplicationWithEligibilityResultAsProperJson() throws Exception {
    // Arrange
    ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .clientDetails(ClientDetailsEntityGenerator.createWithoutId(null))
                    .providerFirmCode(FIRM_CODE));
    ApplicationEntity savedEntity = applicationRepository.save(entity);
    eligibilityResultRepository.saveAndFlush(
        EligibilityResultEntityGenerator.createEligibilityResult(
            savedEntity.getId(), "ELIGIBLE", 100));

    clearCache();

    // Act
    String responseBody =
        mockMvc
            .perform(get("/api/v0/applications/{id}", savedEntity.getId()).withBearerReadToken())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Assert: the raw JsonNode payloads must round-trip as plain JSON, not as
    // serialized JsonNode bean properties (e.g. "object", "nodeType", "containerNode").
    JsonNode eligibilityResult = objectMapper.readTree(responseBody).get("eligibilityResult");
    assertThat(eligibilityResult.get("data"))
        .isEqualTo(objectMapper.readTree("{\"question\":\"answer\"}"));
    assertThat(eligibilityResult.get("result"))
        .isEqualTo(objectMapper.readTree("{\"status\":\"ELIGIBLE\",\"score\":100}"));
  }

  @Test
  void shouldReturnNotFound_whenApplicationDoesNotExist() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/api/v0/applications/{id}", UUID.randomUUID()).withBearerReadToken())
        .andExpect(status().isNotFound());
  }
}
