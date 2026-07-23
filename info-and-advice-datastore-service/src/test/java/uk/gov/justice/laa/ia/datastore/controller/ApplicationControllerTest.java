package uk.gov.justice.laa.ia.datastore.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.config.interceptor.UserContextInterceptor;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.exception.EtagMismatchException;
import uk.gov.justice.laa.ia.datastore.generator.StartApplicationCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.ApplicationSummary;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateMeansDataCommand;
import uk.gov.justice.laa.ia.datastore.service.ApplicationService;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;

/** Unit testing for the {@link ApplicationController}. */
@WebMvcTest({ApplicationController.class, UserContext.class, ObjectMapper.class})
@TestPropertySource(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.security.oauth2.server.resource"
          + ".autoconfigure.OAuth2ResourceServerAutoConfiguration,"
          + "org.springframework.boot.security.oauth2.server.resource"
          + ".autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration"
    })
@ActiveProfiles(profiles = "test")
public class ApplicationControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private UserContextInterceptor userContextInterceptor;

  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @MockitoBean private ApplicationService applicationService;

  @BeforeEach
  void setUp() throws Exception {
    when(userContextInterceptor.preHandle(any(), any(), any())).thenReturn(true);
  }

  @Test
  void startApplication_returnsCreatedStatus_andApplicationIdHeader() throws Exception {
    // Arrange
    StartApplicationCommand command = StartApplicationCommandGenerator.create(null);
    UUID generatedId = UUID.randomUUID();
    when(applicationService.createApplication(any(StartApplicationCommand.class)))
        .thenReturn(generatedId);

    // Act + Assert
    mockMvc
        .perform(
            post("/api/v0/applications:start-application")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(header().string("X-Application-ID", generatedId.toString()));
  }

  @Test
  void getApplications_returnsOkStatus_andApplications() throws Exception {

    // Arrange
    ApplicationSummary application1 =
        new ApplicationSummary(UUID.randomUUID(), "REF-001", null)
            .clientFirstName("Alice")
            .clientLastName("Smith");

    ApplicationSummary application2 =
        new ApplicationSummary(UUID.randomUUID(), "REF-002", null)
            .clientFirstName("Bob")
            .clientLastName("Jones");

    Page<ApplicationSummary> page = new PageImpl<>(List.of(application1, application2));
    when(applicationService.getAllApplications(
            eq(Specification.<ApplicationEntity>unrestricted()), eq(0), eq(25)))
        .thenReturn(page);

    // Act + Assert
    mockMvc
        .perform(get("/api/v0/applications").param("page", "0").param("size", "25"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].id").exists())
        .andExpect(jsonPath("$.content[0].referenceNumber").value("REF-001"))
        .andExpect(jsonPath("$.content[0].clientFirstName").value("Alice"))
        .andExpect(jsonPath("$.content[0].clientLastName").value("Smith"))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(2));
  }

  @Test
  void filterApplications_returnsOkStatus_andFilteredApplications() throws Exception {
    // Arrange
    final UUID officeId = UUID.fromString("019f1461-02ae-71f8-b731-c6d63bb59e6d");
    ApplicationSummary application1 =
        new ApplicationSummary(UUID.randomUUID(), "REF-001", null)
            .clientFirstName("Alice")
            .clientLastName("Smith");

    ApplicationSummary application2 =
        new ApplicationSummary(UUID.randomUUID(), "REF-002", null)
            .clientFirstName("Bob")
            .clientLastName("Jones");

    Page<ApplicationSummary> page = new PageImpl<>(List.of(application1, application2));
    when(applicationService.getAllApplications(any(), any(), any())).thenReturn(page);

    // Act + Assert
    mockMvc
        .perform(
            get("/api/v0/applications")
                .param("page", "0")
                .param("size", "25")
                .param("officeId", officeId.toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.content[0].id").exists());
    verify(applicationService, never())
        .getAllApplications(eq(Specification.<ApplicationEntity>unrestricted()), eq(0), eq(25));
  }

  @Test
  void updateMeansData_returnsOkStatus() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    String body = "{\"eTag\":0,\"some\":\"data\"}";
    when(applicationService.updateMeansData(eq(id), any(UpdateMeansDataCommand.class)))
        .thenReturn(true);

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/" + id + ":update-means-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNoContent());
  }

  @Test
  void updateMeansData_returns404_whenApplicationDoesNotExist() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    String body = "{\"eTag\":0,\"some\":\"data\"}";
    when(applicationService.updateMeansData(eq(id), any(UpdateMeansDataCommand.class)))
        .thenReturn(false);

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
        DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build();
    when(applicationService.updateClientDeclaration(
            eq(applicationId), any(DeclarationCommand.class)))
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
        DeclarationCommand.builder().eTag(0L).declarationConfirmation(true).build();
    when(applicationService.updateClientDeclaration(any(UUID.class), any(DeclarationCommand.class)))
        .thenReturn(false);

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
    String body = "{\"eTag\":0,\"payeIncomeEvidence\":true}";
    when(applicationService.updateEvidence(any(UUID.class), any(UpdateEvidenceCommand.class)))
        .thenReturn(true);

    // Act + Assert
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNoContent());
  }

  @Test
  void updateEvidence_returns404_whenApplicationDoesNotExist() throws Exception {
    // Arrange
    UUID applicationId = UUID.randomUUID();
    String body = "{\"eTag\":0,\"payeIncomeEvidence\":true}";
    when(applicationService.updateEvidence(any(UUID.class), any(UpdateEvidenceCommand.class)))
        .thenReturn(false);

    // Act + Assert
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateMeansData_returns409_whenEtagMismatch() throws Exception {
    // Arrange
    UUID id = UUID.randomUUID();
    String body = "{\"eTag\":2,\"some\":\"data\"}";
    doThrow(new EtagMismatchException())
        .when(applicationService)
        .updateMeansData(eq(id), any(UpdateMeansDataCommand.class));

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/" + id + ":update-means-data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }

  @Test
  void updateDeclaration_returns409_whenEtagMismatch() throws Exception {
    // Arrange
    UUID applicationId = UUID.randomUUID();
    DeclarationCommand declarationCommand =
        DeclarationCommand.builder().eTag(2L).declarationConfirmation(true).build();
    doThrow(new EtagMismatchException())
        .when(applicationService)
        .updateClientDeclaration(eq(applicationId), any(DeclarationCommand.class));

    // Act + Assert
    mockMvc
        .perform(
            put("/api/v0/applications/{id}/declaration", applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(declarationCommand)))
        .andExpect(status().isConflict());
  }

  @Test
  void updateEvidence_returns409_whenEtagMismatch() throws Exception {
    // Arrange
    UUID applicationId = UUID.randomUUID();
    String body = "{\"eTag\":2,\"payeIncomeEvidence\":true}";
    doThrow(new EtagMismatchException())
        .when(applicationService)
        .updateEvidence(eq(applicationId), any(UpdateEvidenceCommand.class));

    // Act + Assert
    mockMvc
        .perform(
            put(TestConstants.UpdateEvidence, applicationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }
}
