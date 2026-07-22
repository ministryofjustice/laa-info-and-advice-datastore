package uk.gov.justice.laa.ia.datastore.controller;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.ia.datastore.api.ApplicationApi;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponses;
import uk.gov.justice.laa.ia.datastore.model.ApplicationSummary;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;
import uk.gov.justice.laa.ia.datastore.service.ApplicationService;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;

/** Controller for handling Application. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ApplicationController implements ApplicationApi {

  private final ApplicationService service;

  @Override
  public ResponseEntity<Void> startCase(StartCaseCommand startCaseCommand) {
    UUID id = service.createApplication(startCaseCommand);

    return ResponseEntity.status(HttpStatus.CREATED)
        .header("X-Application-ID", id.toString())
        .build();
  }

  @Override
  public ResponseEntity<ApplicationResponses> getApplications(
      Integer page, Integer size, UUID officeId) {
    final Specification<ApplicationEntity> filterBy = ApplicationSpecification.filterBy(officeId);
    final Page<ApplicationSummary> result = service.getAllApplications(filterBy, page, size);
    final ApplicationResponses responses =
        new ApplicationResponses()
            .content(result.getContent())
            .page(result.getNumber())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages());
    return ResponseEntity.ok(responses);
  }

  @Override
  public ResponseEntity<ApplicationResponse> getApplication(UUID id) {
    return service
        .getApplication(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  public ResponseEntity<Void> updateMeansData(UUID id, Long ifMatch, Object body) {
    if (service.updateMeansData(id, ifMatch, body)) {
      return ResponseEntity.ok().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<Void> updateDeclaration(
      UUID id, Long ifMatch, DeclarationCommand declarationCommand) {
    if (service.updateClientDeclaration(id, ifMatch, declarationCommand)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<Void> updateEvidence(UUID id, Long ifMatch, Map<String, Object> evidence) {
    if (service.updateEvidence(id, ifMatch, evidence)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
