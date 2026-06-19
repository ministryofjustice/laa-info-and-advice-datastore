package uk.gov.justice.laa.ia.datastore.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.ia.datastore.api.ApplicationApi;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
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
    UUID id = service.createApplication(startCaseCommand);

    return ResponseEntity.status(HttpStatus.CREATED)
        .header("X-Application-ID", id.toString())
        .build();
  }

  @Override
  public ResponseEntity<List<ApplicationResponse>> getApplications(Integer page, Integer size) {

    return ResponseEntity.ok(service.getAllApplications(page, size));
  }

  @Override
  public ResponseEntity<ApplicationResponse> getApplication(UUID id) {
    return service
        .getApplication(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> updateMeansData(UUID id, Object body) {
    if (service.updateMeansData(id, body)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<Void> updateDeclaration(
      UUID id, @Valid DeclarationCommand declarationCommand) {
    if (service.updateClientDeclaration(id, declarationCommand)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
