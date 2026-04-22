package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CaseDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.IndividualEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the ApplicationRepository. */
@WithMockUser()
public class ApplicationRepositoryIntegrationTest extends BaseIntegrationTest {
  @Test
  void shouldGetApplication() {
    final ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.individual(
                  IndividualEntityGenerator.createWithoutId(
                      individualBuilder -> {
                        individualBuilder.address(AddressEntityGenerator.createWithoutId(null));
                      }));
              builder.evidence(EvidenceEntityGenerator.createWithoutId(null));
              builder.caseDetails(CaseDetailsEntityGenerator.createWithoutId(null));
              builder.declaration(DeclarationEntityGenerator.createWithoutId(null));
            });
    final ApplicationEntity savedEntity = applicationRepository.saveAndFlush(entity);
    clearCache();
    final ApplicationEntity getEntity =
        applicationRepository.findById(savedEntity.getId()).orElseThrow();

    assertThat(getEntity)
        .usingRecursiveComparison()
        .isEqualTo(entity)
        .ignoringFields("createdAt", "modifiedAt");
    assertThat(getEntity.getCreatedAt()).isNotNull();
    assertThat(getEntity.getModifiedAt()).isNotNull();
  }
}
