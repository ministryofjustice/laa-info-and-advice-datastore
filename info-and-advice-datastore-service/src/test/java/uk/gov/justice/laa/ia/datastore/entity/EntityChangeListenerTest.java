package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import uk.gov.justice.laa.ia.datastore.entity.listener.EntityChangeListener;

/** Ensures entity lifecycle auditing is configured for all non-audit entities. */
class EntityChangeListenerTest {

  @Test
  void shouldRegisterEntityChangeListenerForAllEntitiesExceptAuditEntities()
      throws ClassNotFoundException {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

    var excludedEntities = Set.of(EventEntity.class.getName(), EntityChangeEntity.class.getName());
    var entities =
        scanner.findCandidateComponents("uk.gov.justice.laa.ia.datastore").stream()
            .filter(entity -> !excludedEntities.contains(entity.getBeanClassName()))
            .toList();

    assertThat(entities).as("Non-audit JPA entities discovered by the classpath scan").isNotEmpty();

    for (var entity : entities) {
      Class<?> entityClass = Class.forName(entity.getBeanClassName());
      EntityListeners listeners = entityClass.getAnnotation(EntityListeners.class);

      assertThat(listeners)
          .as("%s must declare @EntityListeners", entityClass.getName())
          .isNotNull();
      assertThat(listeners.value())
          .as("%s must register EntityChangeListener", entityClass.getName())
          .contains(EntityChangeListener.class);
    }
  }
}
