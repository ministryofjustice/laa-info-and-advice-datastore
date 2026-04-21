package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.IndividualEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the DeclarationRepository. */
@WithMockUser()
public class DeclarationRepositoryIntegrationTest extends BaseIntegrationTest {

  @Autowired private DeclarationRepository repo;

  @Test
  void shouldGetDeclaration() {
    // Arrange
    final UUID individualId =
        individualRepository.save(IndividualEntityGenerator.createWithoutId(null)).getId();
    final UUID applicationId =
        applicationRepository
            .save(ApplicationEntityGenerator.createWithoutId(individualId, null))
            .getId();
    final DeclarationEntity entity =
        DeclarationEntityGenerator.createWithoutId(applicationId, null);
    final DeclarationEntity savedEntity = repo.save(entity);
    clearCache();

    // Act
    final DeclarationEntity getEntity = repo.findById(savedEntity.getId()).orElseThrow();

    // Assert
    assertThat(getEntity)
        .usingRecursiveComparison()
        .isEqualTo(entity)
        .ignoringFields("createdAt", "modifiedAt");
    assertThat(getEntity.getCreatedAt()).isNotNull();
    assertThat(getEntity.getModifiedAt()).isNotNull();
  }
}
