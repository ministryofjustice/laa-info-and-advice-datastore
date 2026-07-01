package uk.gov.justice.laa.ia.datastore.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceGenerator;
import uk.gov.justice.laa.ia.datastore.generator.StartCaseCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.DeclarationResponse;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.service.ApplicationService;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;

/** Unit testing for the {@link ApplicationController}. */
@WebMvcTest(ApplicationController.class)
@TestPropertySource(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.security.oauth2.server.resource"
          + ".autoconfigure.OAuth2ResourceServerAutoConfiguration,"
          + "org.springframework.boot.security.oauth2.server.resource"
          + ".autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration"
    })
public class ApplicationControllerTest {

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @MockitoBean private ApplicationService applicationService;

  @Test
  void startCase_returnsCreatedStatus_andApplicationIdHeader() throws Exception {
    // Arrange
    StartCaseCommand command = StartCaseCommandGenerator.create(null);
    UUID generatedId = UUID.randomUUID();
    when(applicationService.createApplication(any(StartCaseCommand.class))).thenReturn(generatedId);

    // Act + Assert
    mockMvc
        .perform(
            post("/api/v0/applications:start-case")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Application-ID", generatedId.toString()));
  }

  @Test
  void getApplications_returnsOkStatus_andApplications() throws Exception {

    // Arrange
    ApplicationResponse application1 =
        ApplicationResponse.builder()
            .id(UUID.randomUUID())
            .declaration(DeclarationResponse.builder().id(UUID.randomUUID()).build())
            .evidence("EVIDENCE")
            .build();

    ApplicationResponse application2 =
        ApplicationResponse.builder()
            .id(UUID.randomUUID())
            .declaration(DeclarationResponse.builder().id(UUID.randomUUID()).build())
            .evidence("EVIDENCE")
            .build();

    when(applicationService.getAllApplications(
            eq(Specification.<ApplicationEntity>unrestricted()), eq(0), eq(25)))
        .thenReturn(List.of(application1, application2));

    // Act + Assert
    mockMvc
        .perform(get("/api/v0/applications").param("page", "0").param("size", "25"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.*", hasSize(2)))
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].declaration").exists())
        .andExpect(jsonPath("$[0].evidence").exists());
  }

  @Test
  void filterApplications_returnsOkStatus_andFilteredApplications() throws Exception {
    // Arrange
    final UUID officeId = UUID.fromString("019f1461-02ae-71f8-b731-c6d63bb59e6d");
    ApplicationResponse application1 =
        ApplicationResponse.builder()
            .id(UUID.randomUUID())
            .declaration(DeclarationResponse.builder().id(UUID.randomUUID()).build())
            .evidence("EVIDENCE")
            .build();

    ApplicationResponse application2 =
        ApplicationResponse.builder()
            .id(UUID.randomUUID())
            .declaration(DeclarationResponse.builder().id(UUID.randomUUID()).build())
            .evidence("EVIDENCE")
            .build();

    when(applicationService.getAllApplications(any(), any(), any()))
        .thenReturn(List.of(application1, application2));

    // Act + Assert
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("page", "0")
                .param("size", "25")
                .param("officeId", officeId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.*", hasSize(2)))
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].declaration").exists())
        .andExpect(jsonPath("$[0].evidence").exists());
    verify(applicationService, never())
        .getAllApplications(eq(Specification.<ApplicationEntity>unrestricted()), eq(0), eq(25));
  }

  @Test
  void updateMeansData_returnsOkStatus() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    String body = "{\"some\":\"data\"}";
    when(applicationService.updateMeansData(eq(id), any())).thenReturn(true);

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/" + id + ":update-means-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
  }

  @Test
  void updateMeansData_returns404_whenApplicationDoesNotExist() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    String body = "{\"some\":\"data\"}";
    when(applicationService.updateMeansData(eq(id), any())).thenReturn(false);

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/" + id + ":update-means-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateDeclaration_returns204_whenApplicationExists() throws Exception {
    // Arrange
    UUID applicationId = UUID.randomUUID();
    DeclarationCommand declarationCommand =
        DeclarationCommand.builder().declarationConfirmation(true).build();
    when(applicationService.updateClientDeclaration(applicationId, declarationCommand))
        .thenReturn(true);

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/{id}/declaration", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(declarationCommand)))
        .andExpect(status().isNoContent());
  }

  @Test
  void updateDeclaration_returns404_whenApplicationDoesNotExist() throws Exception {
    // Arrange
    UUID applicationId = UUID.randomUUID();
    DeclarationCommand declarationCommand =
        DeclarationCommand.builder().declarationConfirmation(true).build();
    when(applicationService.updateClientDeclaration(any(UUID.class), any())).thenReturn(false);

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/{id}/declaration", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(declarationCommand)))
        .andExpect(status().isNotFound());
  }

  @SuppressWarnings("unchecked")
  @Test
  void updateEvidence_returns204_whenApplicationExists() throws Exception {
    // Arrange
    UUID applicationId = UUID.randomUUID();
    final var updateEvidenceCommand = EvidenceGenerator.createEvidenceMap();
    when(applicationService.updateEvidence(any(UUID.class), any())).thenReturn(true);

    // Act + Assert
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateEvidenceCommand)))
        .andExpect(status().isNoContent());
  }

  @Test
  void updateEvidence_returns404_whenApplicationDoesNotExist() throws Exception {
    // Arrange
    UUID applicationId = UUID.randomUUID();
    var updateEvidenceCommand = EvidenceGenerator.createEvidenceMap();
    when(applicationService.updateEvidence(any(UUID.class), any())).thenReturn(false);

    // Act + Assert
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateEvidenceCommand)))
        .andExpect(status().isNotFound());
  }
}
