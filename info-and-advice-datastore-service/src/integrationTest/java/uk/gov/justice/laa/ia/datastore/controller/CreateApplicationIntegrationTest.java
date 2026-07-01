package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.StartCaseCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/** Integration test for creating an application. */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
public class CreateApplicationIntegrationTest extends BaseIntegrationTest {

  @Autowired private UserContext userContext;

  @Test
  void shouldCreateApplicationSuccessfully() throws Exception {
    // Arrange
    StartCaseCommand command = StartCaseCommandGenerator.create(null);

    // Act
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v0/applications:start-case")
                    .withBearerWriteToken()
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(command)))
            .andExpect(status().isCreated())
            .andExpect(header().exists("X-Application-ID"))
            .andReturn();

    // Assert
    String idString = result.getResponse().getHeader("X-Application-ID");
    UUID applicationId = UUID.fromString(idString);

    clearCache();
    ApplicationEntity savedEntity = applicationRepository.findById(applicationId).orElseThrow();

    assertThat(savedEntity.getClientDetails().getFullName())
        .isEqualTo(command.getClient().getFullName());
    assertThat(savedEntity.getReferenceNumber()).isNotNull();
    assertThat(savedEntity.getReferenceNumber()).matches("L-\\w{3}-\\w{3}");
    assertThat(savedEntity.getCreatedBy()).isEqualTo("SYSTEM");
    assertThat(savedEntity.getProviderOfficeId()).isEqualTo(userContext.getProviderOfficeId());
  }
}
