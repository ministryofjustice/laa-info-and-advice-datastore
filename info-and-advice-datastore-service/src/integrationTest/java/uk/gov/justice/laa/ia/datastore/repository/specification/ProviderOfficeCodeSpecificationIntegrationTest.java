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

/** Integration tests for the ProviderOfficeCodeSpecification. */
@WithMockUser()
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ProviderOfficeCodeSpecificationIntegrationTest extends BaseIntegrationTest {
  @Test
  void whenOfficeIdSpecificationIsUsed_thenReturnApplicationsWithMatchingOfficeId() {
    // Arrange
    String officeId = UUID.randomUUID().toString();
    final ApplicationEntity applicationWithMatchingOfficeId =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.providerOfficeCode(officeId);
            });
    final ApplicationEntity applicationWithDifferentOfficeId =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.providerOfficeCode(UUID.randomUUID().toString());
            });
    applicationRepository.saveAndFlush(applicationWithMatchingOfficeId);
    applicationRepository.saveAndFlush(applicationWithDifferentOfficeId);
    clearCache();

    // create specification
    Specification<ApplicationEntity> specification =
        ApplicationSpecification.filterBy(officeId, null);

    // Act
    List<ApplicationEntity> applications = applicationRepository.findAll(specification);
    // Assert

    assertThat(applications).hasSize(1);
    assertThat(applications.iterator().next().getProviderOfficeCode()).isEqualTo(officeId);
  }
}
