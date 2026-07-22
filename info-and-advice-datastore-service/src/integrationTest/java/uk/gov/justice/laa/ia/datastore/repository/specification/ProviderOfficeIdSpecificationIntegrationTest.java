package uk.gov.justice.laa.ia.datastore.repository.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.specification.ApplicationSpecification;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the ProviderOfficeIdSpecification. */
@WithMockUser()
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ProviderOfficeIdSpecificationIntegrationTest extends BaseIntegrationTest {
  @Test
  void whenOfficeIdSpecificationIsUsed_thenReturnApplicationsWithMatchingOfficeId() {
    // Arrange
    UUID officeId = UUID.randomUUID();
    final ApplicationEntity applicationWithMatchingOfficeId =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.providerOfficeId(officeId);
            });
    final ApplicationEntity applicationWithDifferentOfficeId =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.providerOfficeId(UUID.randomUUID());
            });
    applicationRepository.saveAndFlush(applicationWithMatchingOfficeId);
    applicationRepository.saveAndFlush(applicationWithDifferentOfficeId);
    clearCache();

    // create specification
    Specification<ApplicationEntity> specification = ApplicationSpecification.filterBy(officeId);

    // Act
    List<ApplicationEntity> applications = applicationRepository.findAll(specification);
    // Assert

    assertThat(applications).hasSize(1);
    assertThat(applications.iterator().next().getProviderOfficeId()).isEqualTo(officeId);
  }
}
