package uk.gov.justice.laa.ia.datastore.controller;

import static org.hamcrest.Matchers.hasSize;
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
import uk.gov.justice.laa.ia.datastore.model.Individual;
import uk.gov.justice.laa.ia.datastore.service.IndividualService;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;

/** Unit testing for the {@link IndiviualController}. */
@WebMvcTest(IndividualsController.class)
public class IndividualControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private IndividualService individualService;

  @Test
  void getIndividuals_returnsOkStatus_andItems() throws Exception {
    List<Individual> individuals =
        List.of(
            Individual.builder().individualLegalAidNumber(UUID.randomUUID()).build(),
            Individual.builder().individualLegalAidNumber(UUID.randomUUID()).build());
    when(individualService.getAllIndividuals()).thenReturn(individuals);

    mockMvc
        .perform(get(TestConstants.GetIndividualsApi))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.*", hasSize(2)));
  }
}
