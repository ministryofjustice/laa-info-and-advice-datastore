package uk.gov.justice.laa.ia.datastore.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.ia.datastore.api.ApplicationApi;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.service.ApplicationService;

/** Controller for handling Application. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ApplicationController implements ApplicationApi {

  private final ApplicationService service;

  @Override
  public ResponseEntity<Void> startCase(@Valid StartCaseCommand startCaseCommand) {
    throw new UnsupportedOperationException("Unimplemented method 'startCase'");
  }

  @Override
  public ResponseEntity<List<ApplicationResponse>> getApplications(Integer page, Integer size) {

    return ResponseEntity.ok(service.getAllApplications(page, size));
  }
}
