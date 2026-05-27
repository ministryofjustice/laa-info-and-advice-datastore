package uk.gov.justice.laa.ia.datastore.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.DeclarationResponse;
import uk.gov.justice.laa.ia.datastore.model.EvidenceResponse;
import uk.gov.justice.laa.ia.datastore.service.ApplicationService;

/** Unit testing for the {@link ApplicationController}. */
@WebMvcTest(ApplicationController.class)
public class ApplicationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ApplicationService applicationService;

  @Test
  void getApplications_returnsOkStatus_andApplications() throws Exception {

    // Arrange
    ApplicationResponse application1 =
        ApplicationResponse.builder()
            .id(UUID.randomUUID())
            .declaration(DeclarationResponse.builder().id(UUID.randomUUID()).build())
            .evidence(EvidenceResponse.builder().id(UUID.randomUUID()).build())
            .build();

    ApplicationResponse application2 =
        ApplicationResponse.builder()
            .id(UUID.randomUUID())
            .declaration(DeclarationResponse.builder().id(UUID.randomUUID()).build())
            .evidence(EvidenceResponse.builder().id(UUID.randomUUID()).build())
            .build();

    when(applicationService.getAllApplications(eq(0), eq(25)))
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
}
