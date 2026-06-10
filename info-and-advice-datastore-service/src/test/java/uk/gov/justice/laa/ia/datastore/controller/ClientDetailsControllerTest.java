package uk.gov.justice.laa.ia.datastore.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.justice.laa.ia.datastore.model.ClientDetails;
import uk.gov.justice.laa.ia.datastore.service.ClientDetailsService;
import uk.gov.justice.laa.ia.datastore.utils.TestConstants;

/** Unit testing for the {@link ClientDetailsController}. */
@WebMvcTest(ClientDetailsController.class)
public class ClientDetailsControllerTest {
  @Autowired private MockMvc mockMvc;
  @MockitoBean private ClientDetailsService clientDetailsService;

  @Test
  void getClientDetails_returnsOkStatus_andClientDetails() throws Exception {
    ClientDetails clientDetails =
        ClientDetails.builder().individualLegalAidNumber(UUID.randomUUID()).build();
    when(clientDetailsService.getClientDetails(clientDetails.getIndividualLegalAidNumber()))
        .thenReturn(Optional.of(clientDetails));

    mockMvc
        .perform(get(TestConstants.GetClientDetails, clientDetails.getIndividualLegalAidNumber()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  void getClientDetails_returnNotFound_whenClientDetailsDoesNotExist() throws Exception {
    when(clientDetailsService.getClientDetails(any(UUID.class))).thenReturn(Optional.empty());

    mockMvc
        .perform(get(TestConstants.GetClientDetails, UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }
}
