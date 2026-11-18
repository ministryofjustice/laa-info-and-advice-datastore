package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
