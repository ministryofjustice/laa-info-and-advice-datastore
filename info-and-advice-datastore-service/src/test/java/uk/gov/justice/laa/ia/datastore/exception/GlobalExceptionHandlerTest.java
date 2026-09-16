package uk.gov.justice.laa.ia.datastore.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

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
  void handleMissingLinkedEntityException_returnsBadRequestStatusAndErrorMessage() {
    UUID applicationId = UUID.randomUUID();
    MissingLinkedEntityException exception =
        new MissingLinkedEntityException("declaration", applicationId);

    ResponseEntity<ProblemDetail> result =
        globalExceptionHandler.handleMissingLinkedEntityException(exception);

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(BAD_REQUEST);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody().getStatus()).isEqualTo(BAD_REQUEST.value());
    assertThat(result.getBody().getDetail()).isEqualTo(exception.getMessage());
  }
}
