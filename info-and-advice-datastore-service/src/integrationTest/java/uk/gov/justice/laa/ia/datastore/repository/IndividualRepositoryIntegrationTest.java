package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the ItemController. */
@WithMockUser()
public class IndividualRepositoryIntegrationTest extends BaseIntegrationTest {

  @Autowired private IndividualRepository sut;

  @Test
  void shouldGetIndividual() {
    IndividualEntity entity = createIndividual().build();
    final IndividualEntity savedEntity = sut.save(entity);
    clearCache();
    final IndividualEntity getEntity = sut.findById(savedEntity.getId()).orElseThrow();

    assertThat(getEntity)
        .usingRecursiveComparison()
        .isEqualTo(entity)
        .ignoringFields("createdAt", "modifiedAt");
    assertThat(getEntity.getCreatedAt()).isNotNull();
    assertThat(getEntity.getModifiedAt()).isNotNull();
  }

  private IndividualEntity.IndividualEntityBuilder createIndividual() {
    return IndividualEntity.builder()
        .firstName("Joe")
        .lastName("Bloggs")
        .lastNameAtBirth("Smith")
        .niNumber("AB123456Q")
        .dateOfBirth(LocalDate.of(1990, 01, 01));
  }
}
