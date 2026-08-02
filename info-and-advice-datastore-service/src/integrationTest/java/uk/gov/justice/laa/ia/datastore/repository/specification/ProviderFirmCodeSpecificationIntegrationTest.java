package uk.gov.justice.laa.ia.datastore.repository.specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import lombok.experimental.ExtensionMethod;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the ProviderFirmCodeSpecification. */
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ProviderFirmCodeSpecificationIntegrationTest extends BaseIntegrationTest {
  @Test
  void whenProviderFirmCodeSpecificationIsUsed_thenReturnApplicationsWithMatchingCode() {
    // Arrange
    final String providerFirmCode = "123456";
    final ApplicationEntity applicationWithMatchingProviderFirmCode =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails().providerFirmCode(providerFirmCode);
            });
    final ApplicationEntity applicationWithDifferentProviderFirmCode =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails().providerFirmCode("654321");
            });
    applicationRepository.saveAndFlush(applicationWithMatchingProviderFirmCode);
    applicationRepository.saveAndFlush(applicationWithDifferentProviderFirmCode);
    clearCache();
    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.filterByProviderFirmCode(providerFirmCode);

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).hasSize(1);
    assertThat(applications.iterator().next().getProviderFirmCode()).isEqualTo(providerFirmCode);
  }
}
