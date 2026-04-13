package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generators.IndividualEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.ClientCaseDetailsStatus;
import uk.gov.justice.laa.ia.datastore.model.OverallApplicationStatus;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the ApplicationRepository. */
@WithMockUser()
public class ApplicationRepositoryIntegrationTest extends BaseIntegrationTest {
  @Test
  void shouldGetApplication() {
    final UUID individualId =
        individualRepository.save(IndividualEntityGenerator.createWithoutId(null)).getId();
    final ApplicationEntity entity = createEntity(individualId);
    final ApplicationEntity savedEntity = applicationRepository.save(entity);
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

  private static ApplicationEntity createEntity(UUID individualId) {
    return ApplicationEntity.builder()
        .individualLegalAidNumber(individualId)
        .providerFirmId(UUID.randomUUID())
        .providerOfficeId(UUID.randomUUID())
        .eligibilityResultId(UUID.randomUUID())
        .clientCaseDetailsStatus(ClientCaseDetailsStatus.DRAFT)
        .meansAssessmentStatusId(UUID.randomUUID())
        .evidenceStatusId(UUID.randomUUID())
        .clientDeclarationStatusId(UUID.randomUUID())
        .overallApplicationStatus(OverallApplicationStatus.DRAFT)
        .uniqueFileNumber(UUID.randomUUID())
        .createdBy("Joe Bloggs")
        .modifiedBy("Joe Bloggs")
        .build();
  }
}
