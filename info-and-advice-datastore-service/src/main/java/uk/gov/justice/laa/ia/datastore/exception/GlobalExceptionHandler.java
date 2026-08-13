package uk.gov.justice.laa.ia.datastore.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
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
@Profile("!local") // disable local profiles to allow exceptions to propagate for development
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
