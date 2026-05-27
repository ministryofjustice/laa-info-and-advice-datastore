package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EvidenceEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the ApplicationRepository. */
@WithMockUser()
public class ApplicationRepositoryIntegrationTest extends BaseIntegrationTest {
  @Test
  void shouldGetApplication() {
    final ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.clientDetails(
                  ClientDetailsEntityGenerator.createWithoutId(
                      clientDetailsBuilder -> {
                        clientDetailsBuilder.address(AddressEntityGenerator.createWithoutId(null));
                      }));
              builder.evidence(EvidenceEntityGenerator.createWithoutId(null));
              builder.declaration(DeclarationEntityGenerator.createWithoutId(null));
            });
    final ApplicationEntity savedEntity = applicationRepository.saveAndFlush(entity);
    clearCache();
    final ApplicationEntity getEntity =
        applicationRepository.findById(savedEntity.getId()).orElseThrow();

    assertThat(getEntity)
        .usingRecursiveComparison()
        .ignoringFields("referenceNumber", "createdAt", "modifiedAt")
        .isEqualTo(entity);
    assertThat(getEntity.getCreatedAt()).isNotNull();
    assertThat(getEntity.getModifiedAt()).isNotNull();
    assertThat(getEntity.getReferenceNumber()).isNotNull();
    assertTrue(getEntity.getReferenceNumber().matches(referenceNumberRegex));
  }

  private final String referenceNumberRegex = "L-\\w{3}-\\w{3}";
}
