package uk.gov.justice.laa.ia.datastore.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.ia.datastore.entity.EntityChangeEntity;

/** Writes change records without touching the persistence context that is currently flushing. */
@Repository
public class EntityChangeRepository {

  @PersistenceContext private EntityManager entityManager;

  /** Inserts the record in the current transaction, so it commits or rolls back with the change. */
  public void insert(EntityChangeEntity change) {
    // Share the transaction, but not the persistence context or its pending changes.
    try (Session session =
        entityManager
            .unwrap(Session.class)
            .sessionWithOptions()
            .connection()
            .flushMode(FlushMode.MANUAL)
            .openSession()) {
      session.persist(change);
    }
  }

  /** Records reads independently, including when the caller uses a read-only transaction. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void insertRead(EntityChangeEntity change) {
    insert(change);
  }
}
