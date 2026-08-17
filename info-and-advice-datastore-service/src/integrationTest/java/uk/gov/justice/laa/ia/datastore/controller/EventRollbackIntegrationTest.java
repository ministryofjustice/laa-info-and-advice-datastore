package uk.gov.justice.laa.ia.datastore.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.StartApplicationCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;
import uk.gov.justice.laa.ia.datastore.repository.ApplicationRepository;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;
import uk.gov.justice.laa.ia.datastore.utils.extensions.MockHttpServletRequestBuilderExtensions;

/**
 * Verifies that if a mutation fails mid-transaction, no event row is persisted either.
 *
 * <p>This test must NOT be annotated with {@code @Transactional} so that the real transaction
 * commit/rollback behaviour of the service layer is exercised.
 */
@ExtensionMethod(MockHttpServletRequestBuilderExtensions.class)
@Transactional(propagation = Propagation.NEVER)
public class EventRollbackIntegrationTest extends BaseIntegrationTest {

  @MockitoSpyBean private ApplicationRepository applicationRepository;

  @Test
  void shouldRollbackEvent_whenMutationFails() throws Exception {
    // Arrange — force the application save to throw after the event service has been called
    doThrow(new RuntimeException("Simulated DB failure"))
        .when(applicationRepository)
        .save(any(ApplicationEntity.class));

    StartApplicationCommand command =
        StartApplicationCommandGenerator.create(
            builder -> builder.providerOfficeCode(PROVIDER_OFFICE_CODE));

    // Act
    mockMvc
        .perform(
            post("/api/v0/applications:start-application")
                .withBearerWriteToken()
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(command)))
        .andExpect(status().is5xxServerError());

    // Assert — neither the application nor the event was committed
    assertThat(applicationRepository.findAll()).isEmpty();
    assertThat(eventRepository.findAll()).isEmpty();
  }
}
