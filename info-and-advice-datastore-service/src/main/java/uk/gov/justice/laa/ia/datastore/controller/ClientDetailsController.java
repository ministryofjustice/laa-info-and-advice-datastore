package uk.gov.justice.laa.ia.datastore.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.ia.datastore.api.ClientDetailsApi;
import uk.gov.justice.laa.ia.datastore.model.ClientDetails;
import uk.gov.justice.laa.ia.datastore.service.ClientDetailsService;

/** Controller for handling {@link ClientDetails}. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ClientDetailsController implements ClientDetailsApi {

  private final ClientDetailsService service;

  @Override
  public ResponseEntity<ClientDetails> getClientDetails(UUID id) {
    return service
        .getClientDetails(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
