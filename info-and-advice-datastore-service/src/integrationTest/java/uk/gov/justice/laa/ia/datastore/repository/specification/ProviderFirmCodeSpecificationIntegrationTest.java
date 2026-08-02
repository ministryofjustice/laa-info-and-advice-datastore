package uk.gov.justice.laa.ia.datastore.repository.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the ProviderFirmIdSpecification. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ProviderFirmIdSpecificationIntegrationTest extends BaseIntegrationTest {
  @Test
  void whenProviderFirmIdSpecificationIsUsed_thenReturnApplicationsWithMatchingProviderFirmId() {
    // Arrange
    final UUID providerFirmId = UUID.randomUUID();
    final ApplicationEntity applicationWithMatchingProviderFirmId =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails().providerFirmId(providerFirmId);
            });
    final ApplicationEntity applicationWithDifferentProviderFirmId =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails().providerFirmId(UUID.randomUUID());
            });
    applicationRepository.saveAndFlush(applicationWithMatchingProviderFirmId);
    applicationRepository.saveAndFlush(applicationWithDifferentProviderFirmId);
    clearCache();
    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.filterByProviderFirmId(providerFirmId);

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).hasSize(1);
    assertThat(applications.iterator().next().getProviderFirmId()).isEqualTo(providerFirmId);
  }
}
