package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.EntityChangeEntity.Action;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityAssessmentEntityGenerator;
import uk.gov.justice.laa.ia.datastore.repository.EntitySnapshotRepository;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/**
 * Exercises callbacks, original-state reads and change records against a real persistence context.
 */
class EntityChangeListenerIntegrationTest extends BaseIntegrationTest {

  @Autowired private EntitySnapshotRepository snapshotRepository;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private UserContext userContext;

  @Test
  void recordsRequestIdentityForEveryLifecycleAction() {
    RequestAttributes previous = RequestContextHolder.getRequestAttributes();
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest()));
    try {
      when(userContext.getCurrentUser()).thenReturn("user-123");
      when(userContext.getCorrelationId()).thenReturn("correlation-123");
      ClientDetailsEntity client =
          clientDetailsRepository.saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null));
      final UUID id = client.getId();
      client.setFirstName("updated");
      entityManager.flush();
      entityManager.clear();
      client = clientDetailsRepository.findById(id).orElseThrow();
      clientDetailsRepository.delete(client);
      entityManager.flush();

      assertThat(changesFor(id))
          .extracting(EntityChangeEntity::getAction)
          .containsExactly(Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE);
      assertThat(changesFor(id))
          .allSatisfy(change -> {
            assertThat(change.getUserId()).isEqualTo("user-123");
            assertThat(change.getCorrelationId()).isEqualTo("correlation-123");
          });
    } finally {
      RequestContextHolder.setRequestAttributes(previous);
    }
  }

  @Test
  void recordsSystemIdentityAndLoggingCorrelationOutsideRequest() {
    RequestAttributes previous = RequestContextHolder.getRequestAttributes();
    String previousCorrelationId = MDC.get("correlationId");
    RequestContextHolder.resetRequestAttributes();
    try {
      MDC.put("correlationId", "scheduled-correlation");
      ClientDetailsEntity client =
          clientDetailsRepository.saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null));
      assertThat(changesFor(client.getId()))
          .singleElement()
          .satisfies(change -> {
            assertThat(change.getUserId()).isEqualTo("SYSTEM");
            assertThat(change.getCorrelationId()).isEqualTo("scheduled-correlation");
          });

      MDC.remove("correlationId");
      ClientDetailsEntity withoutCorrelation =
          clientDetailsRepository.saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null));
      assertThat(changesFor(withoutCorrelation.getId()).getFirst().getCorrelationId()).isNull();
    } finally {
      if (previousCorrelationId == null) {
        MDC.remove("correlationId");
      } else {
        MDC.put("correlationId", previousCorrelationId);
      }
      RequestContextHolder.setRequestAttributes(previous);
    }
  }

  @Test
  void recordsCreationLoadAndRemovalWithoutEntityValues() {
    ClientDetailsEntity client =
        clientDetailsRepository.saveAndFlush(
            ClientDetailsEntityGenerator.createWithoutId(builder ->
                builder.lastName(null).address(AddressEntityGenerator.createWithoutId(null))));
    UUID id = client.getId();
    assertThat(changesFor(id))
        .singleElement()
        .satisfies(change -> {
          assertEvent(change, Action.CREATE);
          assertThat(change.getChangedFields())
              .containsExactly(
                  "address",
                  "createdAt",
                  "createdBy",
                  "dateOfBirth",
                  "etag",
                  "firstName",
                  "modifiedAt",
                  "modifiedBy",
                  "niNumber",
                  "noFixedAbode");
        });

    entityManager.clear();
    client = clientDetailsRepository.findById(id).orElseThrow();
    assertThat(changesFor(id))
        .extracting(EntityChangeEntity::getAction)
        .containsExactly(Action.CREATE, Action.READ);
    assertEvent(changesFor(id).getLast(), Action.READ);

    clientDetailsRepository.delete(client);
    entityManager.flush();
    assertThat(changesFor(id))
        .extracting(EntityChangeEntity::getAction)
        .containsExactly(Action.CREATE, Action.READ, Action.DELETE);
    assertEvent(changesFor(id).getLast(), Action.DELETE);
  }

  @Test
  @Transactional(Transactional.TxType.NOT_SUPPORTED)
  void recordsReadsInReadOnlyTransaction() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    UUID id =
        transaction.execute(status ->
            clientDetailsRepository
                .saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null))
                .getId());
    try {
      TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
      readOnly.setReadOnly(true);
      readOnly.executeWithoutResult(status -> {
        assertThat(clientDetailsRepository.findById(id)).isPresent();
        status.setRollbackOnly();
      });
      assertThat(changesFor(id))
          .extracting(EntityChangeEntity::getAction)
          .containsExactly(Action.CREATE, Action.READ);
      transaction.executeWithoutResult(status -> {
        clientDetailsRepository.deleteById(id);
        clientDetailsRepository.flush();
        status.setRollbackOnly();
      });
      assertThat(changesFor(id))
          .extracting(EntityChangeEntity::getAction)
          .containsExactly(Action.CREATE, Action.READ, Action.READ);
    } finally {
      transaction.executeWithoutResult(status -> {
        clientDetailsRepository.deleteById(id);
        clientDetailsRepository.flush();
        entityManager
            .createQuery("delete from EntityChangeEntity c where c.entityId = :id")
            .setParameter("id", id)
            .executeUpdate();
      });
    }
  }

  @Test
  @Transactional(Transactional.TxType.NOT_SUPPORTED)
  void rollsBackCreationAuditWithTheEntity() {
    TransactionTemplate transaction = new TransactionTemplate(transactionManager);
    UUID id =
        transaction.execute(status -> {
          UUID createdId =
              clientDetailsRepository
                  .saveAndFlush(ClientDetailsEntityGenerator.createWithoutId(null))
                  .getId();
          status.setRollbackOnly();
          return createdId;
        });
    assertThat(changesFor(id)).isEmpty();
    assertThat(clientDetailsRepository.findById(id)).isEmpty();
  }

  @Test
  void recordsChangedFieldNamesForAllSixEntitiesWithoutValues() {
    ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(builder -> {
          ApplicationEntityBuilderExtensions.withDefaultClientDetails(builder);
          builder.declaration(DeclarationEntityGenerator.createWithoutId(null));
          builder.evidence(
              EvidenceEntity.builder().createdBy("SYSTEM").modifiedBy("SYSTEM").build());
          builder.scopingQuestions(objectMapper.createObjectNode().put("private", "sensitive"));
        });
    applicationRepository.saveAndFlush(application);
    final EligibilityAssessmentEntity assessment =
        eligibilityAssessmentRepository.saveAndFlush(
            EligibilityAssessmentEntityGenerator.createEligibilityAssessment(application.getId()));
    assertThat(
            entityManager
                .createQuery(
                    "select c.entityType from EntityChangeEntity c where c.action = :action",
                    String.class)
                .setParameter("action", Action.CREATE)
                .getResultList())
        .containsExactlyInAnyOrder(
            "ApplicationEntity",
            "ClientDetailsEntity",
            "AddressEntity",
            "DeclarationEntity",
            "EvidenceEntity",
            "EligibilityAssessmentEntity");
    assertThat(changesFor(application.getId()).getFirst().getChangedFields())
        .contains("clientDetails", "scopingQuestions")
        .doesNotContain("id", "laaReference", "eligibilityAssessments", "sensitive");
    entityManager.refresh(application);
    entityManager.refresh(assessment);
    assertThat(changes()).isEmpty();
    final long readsBeforeUpdate = readCount();

    application.setScopingQuestions(
        objectMapper.createObjectNode().put("private", "updated sensitive"));
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
    assertThat(changes())
        .hasSize(6)
        .allSatisfy(change -> assertThat(change.getEntityId()).isNotNull());
    assertThat(readCount()).isEqualTo(readsBeforeUpdate);

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

    client.setFirstName("Joe");
    client.setLastName(null);
    entityManager.flush();

    assertThat(changes()).hasSize(2);
    assertThat(changes().getLast().getChangedFields()).containsExactly("firstName", "lastName");
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
  }

  @Test
  void recordsCollectionFieldWhenItsSizeChanges() {
    ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            ApplicationEntityBuilderExtensions::withDefaultClientDetails);
    applicationRepository.saveAndFlush(application);
    entityManager.refresh(application);

    application.setEligibilityAssessments(
        Set.of(
            EligibilityAssessmentEntityGenerator.createEligibilityAssessment(application.getId())));
    entityManager.flush();

    assertChanged("ApplicationEntity", "eligibilityAssessments");
  }

  @Test
  void doesNotRecordUnchangedEntitiesOrEventUpdates() {
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

    assertThat(changes()).isEmpty();
    assertThat(EventEntity.class.getAnnotation(jakarta.persistence.EntityListeners.class)).isNull();
  }

  @Test
  void failsExplicitlyIfTheOriginalEntityDoesNotExist() {
    assertThatThrownBy(
            () -> snapshotRepository.findOriginal(ClientDetailsEntity.class, UUID.randomUUID()))
        .hasRootCauseInstanceOf(EntityNotFoundException.class);
  }

  private List<EntityChangeEntity> changes() {
    return entityManager
        .createQuery(
            "select c from EntityChangeEntity c where c.action = :action order by c.id",
            EntityChangeEntity.class)
        .setParameter("action", Action.UPDATE)
        .getResultList();
  }

  private List<EntityChangeEntity> changesFor(UUID id) {
    return entityManager
        .createQuery(
            "select c from EntityChangeEntity c where c.entityId = :id order by c.id",
            EntityChangeEntity.class)
        .setParameter("id", id)
        .getResultList();
  }

  private long readCount() {
    return entityManager
        .createQuery(
            "select count(c) from EntityChangeEntity c where c.action = :action", Long.class)
        .setParameter("action", Action.READ)
        .getSingleResult();
  }

  private void assertEvent(EntityChangeEntity change, Action action) {
    assertThat(change.getAction()).isEqualTo(action);
    assertThat(change.getEntityType()).isEqualTo("ClientDetailsEntity");
    if (action != Action.CREATE) {
      assertThat(change.getChangedFields()).isEmpty();
    }
    assertThat(change.getCreatedAt()).isNotNull();
  }

  private void assertChanged(String entityType, String... expectedFields) {
    List<EntityChangeEntity> entries =
        changes().stream().filter(change -> entityType.equals(change.getEntityType())).toList();
    assertThat(entries).hasSize(1);
    assertThat(entries.getFirst().getChangedFields()).isEqualTo(List.of(expectedFields));
  }
}
