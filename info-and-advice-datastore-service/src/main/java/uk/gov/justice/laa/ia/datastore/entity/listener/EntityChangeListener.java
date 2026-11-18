package uk.gov.justice.laa.ia.datastore.entity.listener;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.entity.EntityChangeEntity;
import uk.gov.justice.laa.ia.datastore.entity.EntityChangeEntity.Action;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.repository.EntityChangeRepository;
import uk.gov.justice.laa.ia.datastore.repository.EntitySnapshotRepository;

/** Records lifecycle events and changed field names, never entity values. */
@Component
public class EntityChangeListener {

  private static final ThreadLocal<Boolean> READING_SNAPSHOT = new ThreadLocal<>();

  private final EntitySnapshotRepository repository;

  private final EntityChangeRepository changeRepository;

  private final EntityManager entityManager;

  /** Defers persistence dependencies until after Hibernate has registered its listeners. */
  public EntityChangeListener(
      @Lazy EntitySnapshotRepository repository,
      @Lazy EntityChangeRepository changeRepository,
      @Lazy EntityManager entityManager) {
    this.repository = repository;
    this.changeRepository = changeRepository;
    this.entityManager = entityManager;
  }

  /** Records creation after the generated identifier is available. */
  @PostPersist
  public void postPersist(Object entity) {
    if (entity instanceof EventEntity) {
      return;
    }
    BeanWrapper created = new BeanWrapperImpl(entity);
    var changedFields =
        entityManager
            .getMetamodel()
            .entity(Hibernate.getClass(entity))
            .getSingularAttributes()
            .stream()
            .filter(attribute -> !attribute.isId())
            .filter(attribute -> created.getPropertyValue(attribute.getName()) != null)
            .map(SingularAttribute::getName)
            .sorted()
            .toList();
    record(entity, Action.CREATE, changedFields);
  }

  /** Records database loads, excluding internal reads used for update comparisons. */
  @PostLoad
  public void postLoad(Object entity) {
    if (!Boolean.TRUE.equals(READING_SNAPSHOT.get())) {
      record(entity, Action.READ, List.of());
    }
  }

  /** Records removal in the same transaction as the entity. */
  @PreRemove
  public void preRemove(Object entity) {
    record(entity, Action.DELETE, List.of());
  }

  /** Compares the pending update with its stored state before Hibernate writes it. */
  @PreUpdate
  public void preUpdate(Object entity) {
    if (entity instanceof EventEntity) {
      return;
    }

    Class<?> entityType = Hibernate.getClass(entity);
    PersistenceUnitUtil persistenceUnitUtil =
        entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
    Object id = persistenceUnitUtil.getIdentifier(entity);
    Object snapshot;
    READING_SNAPSHOT.set(true);
    try {
      snapshot = repository.findOriginal(entityType, id);
    } finally {
      READING_SNAPSHOT.remove();
    }
    BeanWrapper original = new BeanWrapperImpl(snapshot);
    BeanWrapper updated = new BeanWrapperImpl(entity);
    var changedFields =
        entityManager.getMetamodel().entity(entityType).getSingularAttributes().stream()
            .filter(attribute -> !attribute.isId())
            .filter(attribute -> changed(attribute, original, updated, persistenceUnitUtil))
            .map(SingularAttribute::getName)
            .sorted()
            .toList();

    if (!changedFields.isEmpty()) {
      record(entity, Action.UPDATE, changedFields);
    }
  }

  private void record(Object entity, Action action, List<String> changedFields) {
    if (entity instanceof EventEntity) {
      return;
    }
    EntityChangeEntity change =
        EntityChangeEntity.builder()
            .entityType(Hibernate.getClass(entity).getSimpleName())
            .entityId(
                (UUID)
                    entityManager
                        .getEntityManagerFactory()
                        .getPersistenceUnitUtil()
                        .getIdentifier(entity))
            .action(action)
            .changedFields(changedFields)
            .build();
    if (action == Action.READ) {
      changeRepository.insertRead(change);
    } else {
      changeRepository.insert(change);
    }
  }

  private boolean changed(
      SingularAttribute<?, ?> attribute,
      BeanWrapper original,
      BeanWrapper updated,
      PersistenceUnitUtil persistenceUnitUtil) {
    Object before = original.getPropertyValue(attribute.getName());
    Object after = updated.getPropertyValue(attribute.getName());
    if (attribute.isAssociation()) {
      before = before == null ? null : persistenceUnitUtil.getIdentifier(before);
      after = after == null ? null : persistenceUnitUtil.getIdentifier(after);
    }
    return !Objects.equals(before, after);
  }
}
