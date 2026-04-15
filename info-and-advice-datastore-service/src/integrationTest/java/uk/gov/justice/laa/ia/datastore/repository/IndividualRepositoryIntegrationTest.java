package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;
import uk.gov.justice.laa.ia.datastore.generator.IndividualEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the IndividualRepository. */
@WithMockUser()
public class IndividualRepositoryIntegrationTest extends BaseIntegrationTest {

  @Test
  void shouldGetIndividual() {
    IndividualEntity entity = IndividualEntityGenerator.createWithoutId(null);
    final IndividualEntity savedEntity = individualRepository.save(entity);
    clearCache();
    final IndividualEntity getEntity =
        individualRepository.findById(savedEntity.getId()).orElseThrow();

    assertThat(getEntity)
        .usingRecursiveComparison()
        .isEqualTo(entity)
        .ignoringFields("createdAt", "modifiedAt");
    assertThat(getEntity.getCreatedAt()).isNotNull();
    assertThat(getEntity.getModifiedAt()).isNotNull();
  }
}
