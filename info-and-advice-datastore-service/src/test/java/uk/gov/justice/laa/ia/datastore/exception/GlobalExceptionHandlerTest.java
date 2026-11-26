package uk.gov.justice.laa.ia.datastore.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GONE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.ia.datastore.audit.AuditLogger;
import uk.gov.justice.laa.ia.datastore.context.UserContext;

class GlobalExceptionHandlerTest {

  GlobalExceptionHandler globalExceptionHandler =
      new GlobalExceptionHandler(mock(AuditLogger.class), mock(UserContext.class));

  @Test
  void handleGenericException_returnsInternalServerErrorStatusAndErrorMessage() throws Exception {
    ResponseEntity<ProblemDetail> result =
        globalExceptionHandler.handleGenericException(new RuntimeException("Something went wrong"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
    assertThat(result.getBody().getDetail())
        .isEqualTo("An unexpected application error has occurred.");
  }

  @Test
  void handleDuplicateUfnException_returnsConflictStatusAndErrorMessage() {
    DuplicateUfnException exception = new DuplicateUfnException("123456/1", "OFFICE1");

    ResponseEntity<ProblemDetail> result =
        globalExceptionHandler.handleDuplicateUfnException(exception);

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getStatus()).isEqualTo(CONFLICT.value());
    assertThat(result.getBody().getDetail()).isEqualTo(exception.getMessage());
    assertThat(result.getBody().getProperties()).containsEntry("reason", "CONFLICT_UFN_DUPLICATE");
  }

  @Test
  void handleDataIntegrityViolationException_returnsDuplicateUfnReasonForUfnConstraint() {
    ConstraintViolationException cause = mock(ConstraintViolationException.class);
    when(cause.getConstraintName()).thenReturn("uq_applications_office_code_ufn");

    ResponseEntity<ProblemDetail> result =
        globalExceptionHandler.handleDataIntegrityViolationException(
            new DataIntegrityViolationException("constraint violation", cause));

    assertThat(result.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getDetail())
        .isEqualTo("UFN already exists for this provider office code.");
    assertThat(result.getBody().getProperties()).containsEntry("reason", "CONFLICT_UFN_DUPLICATE");
  }

  @Test
  void handleDataIntegrityViolationException_doesNotIdentifyOtherConstraintsAsDuplicateUfn() {
    ConstraintViolationException cause = mock(ConstraintViolationException.class);
    when(cause.getConstraintName()).thenReturn("another_constraint");

    ResponseEntity<ProblemDetail> result =
        globalExceptionHandler.handleDataIntegrityViolationException(
            new DataIntegrityViolationException("constraint violation", cause));

    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getDetail())
        .isEqualTo("Conflict: the request violates a data integrity constraint");
    assertThat(result.getBody().getProperties()).isNull();
  }

  @Test
  void handleDataIntegrityViolationException_returnsConflictStatusAndErrorMessage() {
    ResponseEntity<ProblemDetail> result =
        globalExceptionHandler.handleDataIntegrityViolationException(
            new DataIntegrityViolationException("constraint violation"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getStatus()).isEqualTo(CONFLICT.value());
    assertThat(result.getBody().getDetail())
        .isEqualTo("Conflict: the request violates a data integrity constraint");
  }

  @Test
  void handleApplicationDeletedException_returnsGoneStatusAndErrorMessage() {
    UUID applicationId = UUID.randomUUID();
    ApplicationDeletedException exception = new ApplicationDeletedException(applicationId);

    ResponseEntity<ProblemDetail> result =
        globalExceptionHandler.handleApplicationDeletedException(exception);

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(GONE);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getStatus()).isEqualTo(GONE.value());
    assertThat(result.getBody().getDetail()).isEqualTo(exception.getMessage());
  }
}
