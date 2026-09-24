package uk.gov.justice.laa.ia.datastore.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** The global exception handler for all exceptions. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  /**
   * The handler for EtagMismatchException.
   *
   * @param exception the exception
   * @return 409 Conflict response
   */
  @ExceptionHandler(EtagMismatchException.class)
  public ResponseEntity<ProblemDetail> handleEtagMismatchException(
      EtagMismatchException exception) {
    log.warn("ETag mismatch: {}", exception.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * The handler for OptimisticLockingFailureException — thrown by JPA when a concurrent
   * modification is detected via the {@code @Version} field.
   *
   * @param exception the exception
   * @return 409 Conflict response
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(
      OptimisticLockingFailureException exception) {
    log.warn("Concurrent modification conflict: {}", exception.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "Conflict: resource was modified concurrently");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * The handler for DuplicateUfnException.
   *
   * @param exception the exception
   * @return 409 Conflict response
   */
  @ExceptionHandler(DuplicateUfnException.class)
  public ResponseEntity<ProblemDetail> handleDuplicateUfnException(
      DuplicateUfnException exception) {
    log.warn("Duplicate UFN: {}", exception.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * The handler for DeclarationAlreadySignedException.
   *
   * @param exception the exception
   * @return 409 Conflict response
   */
  @ExceptionHandler(DeclarationAlreadySignedException.class)
  public ResponseEntity<ProblemDetail> handleDeclarationAlreadySignedException(
      DeclarationAlreadySignedException exception) {
    log.warn("Declaration already signed: {}", exception.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * The handler for DataIntegrityViolationException — a safety net for database-level constraint
   * violations (e.g. a concurrent request creating a duplicate UFN) that were not caught by
   * application-level validation.
   *
   * @param exception the exception
   * @return 409 Conflict response
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
      DataIntegrityViolationException exception) {
    log.warn("Data integrity violation: {}", exception.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, "Conflict: the request violates a data integrity constraint");
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
  }

  /**
   * The handler for ProviderOfficeNotAuthorizedException.
   *
   * @param exception the exception
   * @return 403 Forbidden response
   */
  @ExceptionHandler(ProviderOfficeNotAuthorizedException.class)
  public ResponseEntity<ProblemDetail> handleProviderOfficeNotAuthorizedException(
      ProviderOfficeNotAuthorizedException exception) {
    log.warn("Provider office not authorized: {}", exception.getMessage());
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
  }

  /**
   * Handles invalid client details patches.
   *
   * @param exception the invalid patch exception
   * @return 400 Bad Request response
   */
  @ExceptionHandler(InvalidClientDetailsPatchException.class)
  public ResponseEntity<ProblemDetail> handleInvalidClientDetailsPatch(
      InvalidClientDetailsPatchException exception) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    return ResponseEntity.badRequest().body(problemDetail);
  }

  /**
   * The handler for Exception.
   *
   * @param exception the exception
   * @return the response status with error message
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericException(Exception exception) {
    String logMessage = "An unexpected application error has occurred.";
    log.error(logMessage, exception);
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    return ResponseEntity.internalServerError().body(problemDetail);
  }
}
