package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.CaseDetailsEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CaseDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.IndividualEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the EvidenceRepository. */
@WithMockUser()
public class CaseDetailsRepositoryIntegrationTest extends BaseIntegrationTest {

  @Autowired private CaseDetailsRepository repo;

  @Test
  void shouldGetCaseDetails() {
    // Arrange
    final UUID individualId =
        individualRepository.save(IndividualEntityGenerator.createWithoutId(null)).getId();
    final UUID applicationId =
        applicationRepository
            .save(ApplicationEntityGenerator.createWithoutId(individualId, null))
            .getId();
    final CaseDetailsEntity entity =
        CaseDetailsEntityGenerator.createWithoutId(applicationId, null);
    final CaseDetailsEntity savedEntity = repo.save(entity);

    // Act
    final CaseDetailsEntity getEntity = repo.findById(savedEntity.getId()).orElseThrow();

    // Assert
    assertThat(getEntity)
        .usingRecursiveComparison()
        .isEqualTo(entity)
        .ignoringFields("createdAt", "modifiedAt");
    assertThat(getEntity.getCreatedAt()).isNotNull();
    assertThat(getEntity.getModifiedAt()).isNotNull();
  }
}
