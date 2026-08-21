package uk.gov.justice.laa.ia.datastore.controller;

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
import uk.gov.justice.laa.ia.datastore.model.ApplicationState;
import uk.gov.justice.laa.ia.datastore.model.ApplicationSummary;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.EligibilityIndication;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateMeansDataCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateScopingDataCommand;
import uk.gov.justice.laa.ia.datastore.service.ApplicationService;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;

/** Controller for handling Application. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class ApplicationController implements ApplicationApi {

  private final ApplicationService service;

  @Override
  public ResponseEntity<ApplicationResponse> startApplication(
      StartApplicationCommand startApplicationCommand) {
    ApplicationResponse response = service.createApplication(startApplicationCommand);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<ApplicationResponses> getApplications(
      Integer page,
      Integer size,
      String officeId,
      ApplicationState status,
      EligibilityIndication eligibilityIndication) {
    final Specification<ApplicationEntity> filterBy =
        ApplicationSpecification.filterBy(officeId, status, eligibilityIndication);
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
  public ResponseEntity<Void> updateMeansData(
      UUID id, UpdateMeansDataCommand updateMeansDataCommand) {
    if (service.updateMeansData(id, updateMeansDataCommand)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<Void> updateDeclarationData(
      UUID id, DeclarationCommand declarationCommand) {
    if (service.updateClientDeclaration(id, declarationCommand)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<Void> updateEvidence(UUID id, UpdateEvidenceCommand updateEvidenceCommand) {
    if (service.updateEvidence(id, updateEvidenceCommand)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<Void> updateScopingData(
      UUID id, UpdateScopingDataCommand updateScopingDataCommand) {
    if (service.updateScopingData(id, updateScopingDataCommand)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @Override
  public ResponseEntity<Void> updateApplication(
      UUID id, UpdateApplicationCommand updateApplicationCommand) {
    if (service.updateApplication(id, updateApplicationCommand)) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
