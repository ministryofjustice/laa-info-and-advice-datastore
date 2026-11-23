package uk.gov.justice.laa.ia.datastore.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

/** Reads stored entities without returning an already-modified managed instance. */
@Repository
public class EntitySnapshotRepository {

  @PersistenceContext private EntityManager entityManager;

  /** Loads the database state, including earlier writes in the current transaction. */
  public <T> T findOriginal(Class<T> entityType, Object id) {
    // Share the transaction, but not the persistence context or its pending changes.
    try (Session session =
        entityManager
            .unwrap(Session.class)
            .sessionWithOptions()
            .connection()
            .flushMode(FlushMode.MANUAL)
            .readOnly(true)
            .initialCacheMode(CacheMode.IGNORE)
            .openSession()) {
      T original = session.find(entityType, id);
      if (original == null) {
        throw new EntityNotFoundException(
            "Cannot compare missing " + entityType.getSimpleName() + " with ID " + id);
      }
      return original;
    }
  }
}
