package uk.gov.justice.laa.ia.datastore.entity.listener;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.ia.datastore.entity.EventEntity;
import uk.gov.justice.laa.ia.datastore.repository.EntitySnapshotRepository;

/** Logs changed persistent field names only; entity values may contain personal data. */
@Component
@Slf4j
public class EntityChangeListener {

  private final EntitySnapshotRepository repository;

  private final EntityManager entityManager;

  /** Defers persistence dependencies until after Hibernate has registered its listeners. */
  public EntityChangeListener(
      @Lazy EntitySnapshotRepository repository, @Lazy EntityManager entityManager) {
    this.repository = repository;
    this.entityManager = entityManager;
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
    BeanWrapper original = new BeanWrapperImpl(repository.findOriginal(entityType, id));
    BeanWrapper updated = new BeanWrapperImpl(entity);
    var changedFields =
        entityManager.getMetamodel().entity(entityType).getSingularAttributes().stream()
            .filter(attribute -> !attribute.isId())
            .filter(attribute -> changed(attribute, original, updated, persistenceUnitUtil))
            .map(SingularAttribute::getName)
            .sorted()
            .toList();

    if (!changedFields.isEmpty()) {
      log.atInfo()
          .addKeyValue("event.action", "entity.preUpdate")
          .addKeyValue("resource.type", entityType.getSimpleName())
          .addKeyValue("resource.id", id)
          .addKeyValue("labels.changedFields", changedFields)
          .log("Entity update pending");
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
