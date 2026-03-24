package uk.gov.justice.laa.ia.datastore.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.ia.datastore.api.IndividualsApi;
import uk.gov.justice.laa.ia.datastore.model.Individual;
import uk.gov.justice.laa.ia.datastore.service.IndividualService;

/** Controller for handling {@link Individual}. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class IndividualsController implements IndividualsApi {

  private final IndividualService service;

  @Override
  public ResponseEntity<List<Individual>> getIndividuals() {
    return ResponseEntity.ok(service.getAllIndividuals());
  }
}
