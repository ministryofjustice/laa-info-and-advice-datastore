package uk.gov.justice.laa.ia.datastore.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.TestJwtConfig;

/** Integration test for getting an application. */
public class GetApplicationIntegrationTest extends BaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldGetApplicationSuccessfully() throws Exception {
    // Arrange
    ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.clientDetails(ClientDetailsEntityGenerator.createWithoutId(null));
              builder.referenceNumber("L-ABC-123");
            });
    ApplicationEntity savedEntity = applicationRepository.save(entity);

    // Act & Assert
    mockMvc
        .perform(
            get("/api/v0/applications/{id}", savedEntity.getId())
                .header("Authorization", "Bearer " + TestJwtConfig.READ_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(savedEntity.getId().toString()))
        .andExpect(
            jsonPath("$.individualLegalAidNumber")
                .value(savedEntity.getClientDetails().getId().toString()))
        .andExpect(jsonPath("$.applicationType").value(savedEntity.getApplicationType()));
  }

  @Test
  void shouldReturnNotFound_whenApplicationDoesNotExist() throws Exception {
    // Act & Assert
    mockMvc
        .perform(
            get("/api/v0/applications/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + TestJwtConfig.READ_TOKEN))
        .andExpect(status().isNotFound());
  }
}
