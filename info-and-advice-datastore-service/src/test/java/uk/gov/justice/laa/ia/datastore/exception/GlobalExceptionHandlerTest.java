package uk.gov.justice.laa.ia.datastore.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void handleGenericException_returnsInternalServerErrorStatusAndErrorMessage() throws Exception {
    ResponseEntity<String> result =
        globalExceptionHandler.handleGenericException(new RuntimeException("Something went wrong"));

    assertThat(result).isNotNull();
    assertThat(result.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR);
    assertThat(result.getBody()).isNotNull();
    assertThat(result.getBody()).isEqualTo("An unexpected application error has occurred.");
  }
}
