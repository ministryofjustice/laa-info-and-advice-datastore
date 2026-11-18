package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.entity.listener.EntityChangeListener;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityAssessmentEntityGenerator;
import uk.gov.justice.laa.ia.datastore.repository.EntitySnapshotRepository;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Exercises callbacks and original-state reads against a real persistence context. */
class EntityChangeListenerIntegrationTest extends BaseIntegrationTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(EntityChangeListener.class);
  private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

  @Autowired private EntitySnapshotRepository snapshotRepository;

  @BeforeEach
  void attachLogger() {
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachLogger() {
    logger.detachAppender(appender);
    appender.stop();
  }

  @Test
  void logsChangesForAllSixEntitiesWithoutLoggingValues() {
    ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(builder -> {
          ApplicationEntityBuilderExtensions.withDefaultClientDetails(builder);
          builder.declaration(DeclarationEntityGenerator.createWithoutId(null));
          builder.evidence(
              EvidenceEntity.builder().createdBy("SYSTEM").modifiedBy("SYSTEM").build());
        });
    applicationRepository.saveAndFlush(application);
    EligibilityAssessmentEntity assessment =
        eligibilityAssessmentRepository.saveAndFlush(
            EligibilityAssessmentEntityGenerator.createEligibilityAssessment(application.getId()));
    entityManager.refresh(application);
    entityManager.refresh(assessment);
    assertThat(appender.list).isEmpty();

    application.setScopingQuestions(objectMapper.createObjectNode().put("private", "sensitive"));
    application.getClientDetails().setFirstName(null);
    application.getClientDetails().setNoFixedAbode(true);
    application.getClientDetails().getAddress().setAddressLine1("sensitive address");
    application.getDeclaration().setDateSigned(LocalDate.of(2026, 1, 1));
    application.getEvidence().setEvidenceExemptionReason("sensitive reason");
    ((ObjectNode) assessment.getData()).put("private", "sensitive assessment");
    entityManager.flush();

    assertChanged("ApplicationEntity", "scopingQuestions");
    assertChanged("ClientDetailsEntity", "firstName", "noFixedAbode");
    assertChanged("AddressEntity", "addressLine1");
    assertChanged("DeclarationEntity", "dateSigned");
    assertChanged("EvidenceEntity", "evidenceExemptionReason");
    assertChanged("EligibilityAssessmentEntity", "data");
    assertThat(appender.list).hasSize(6);
    for (ILoggingEvent event : appender.list) {
      assertThat(event.getFormattedMessage()).isEqualTo("Entity update pending");
      assertThat(fields(event))
          .containsEntry("event.action", "entity.preUpdate")
          .containsKey("resource.id")
          .hasSize(4);
    }

    clearCache();
    assertThat(
            applicationRepository.findById(application.getId()).orElseThrow().getScopingQuestions())
        .isEqualTo(application.getScopingQuestions());
  }

  @Test
  void comparesAgainstThePreviousFlushInTheSameTransaction() {
    ClientDetailsEntity client =
        clientDetailsRepository.saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null));
    entityManager.refresh(client);
    client.setFirstName("updated first name");
    entityManager.flush();
    assertChanged("ClientDetailsEntity", "firstName");
    entityManager.refresh(client);
    appender.list.clear();

    client.setFirstName("Joe");
    client.setLastName(null);
    entityManager.flush();

    assertChanged("ClientDetailsEntity", "firstName", "lastName");
    assertThat(appender.list).hasSize(1);
    clearCache();
    ClientDetailsEntity stored = clientDetailsRepository.findById(client.getId()).orElseThrow();
    assertThat(stored.getFirstName()).isEqualTo("Joe");
    assertThat(stored.getLastName()).isNull();
  }

  @Test
  void comparesRelationshipIdsAndIgnoresUnchangedFieldsDuringMerge() {
    ClientDetailsEntity client =
        clientDetailsRepository.saveAndFlush(
            ClientDetailsEntityGenerator.createWithoutId(
                builder -> builder.address(AddressEntityGenerator.createWithoutId(null))));
    entityManager.refresh(client);
    clearCache();
    client.setAddress(AddressEntityGenerator.createWithoutId(null));

    clientDetailsRepository.saveAndFlush(client);

    assertChanged("ClientDetailsEntity", "address");
    assertThat(appender.list).hasSize(1);
  }

  @Test
  void doesNotLogUnchangedEntitiesOrEventUpdates() {
    clientDetailsRepository.saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null));
    EventEntity event =
        eventRepository.saveAndFlush(
            EventEntity.builder()
                .changedBy("SYSTEM")
                .serviceName("test")
                .httpMethod("PATCH")
                .urlPath("/test")
                .build());
    event.setPayload(objectMapper.createObjectNode().put("private", "sensitive"));
    entityManager.flush();

    assertThat(appender.list).isEmpty();
    assertThat(EventEntity.class.getAnnotation(jakarta.persistence.EntityListeners.class)).isNull();
  }

  @Test
  void failsExplicitlyIfTheOriginalEntityDoesNotExist() {
    assertThatThrownBy(
            () -> snapshotRepository.findOriginal(ClientDetailsEntity.class, UUID.randomUUID()))
        .hasRootCauseInstanceOf(EntityNotFoundException.class);
  }

  private void assertChanged(String entityType, String... expectedFields) {
    List<Map<String, Object>> entries =
        appender.list.stream()
            .map(this::fields)
            .filter(entry -> entityType.equals(entry.get("resource.type")))
            .toList();
    assertThat(entries).hasSize(1);
    assertThat(entries.getFirst().get("labels.changedFields")).isEqualTo(List.of(expectedFields));
  }

  private Map<String, Object> fields(ILoggingEvent event) {
    return event.getKeyValuePairs().stream()
        .collect(Collectors.toMap(pair -> pair.key, pair -> pair.value));
  }
}
