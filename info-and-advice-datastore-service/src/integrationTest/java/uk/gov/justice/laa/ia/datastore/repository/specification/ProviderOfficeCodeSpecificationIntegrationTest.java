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
        ApplicationSpecification.filterBy(officeId, null, null);

    // Act
    List<ApplicationEntity> applications = applicationRepository.findAll(specification);
    // Assert

    assertThat(applications).hasSize(1);
    assertThat(applications.iterator().next().getProviderOfficeCode()).isEqualTo(officeId);
  }

  @Test
  void whenProviderOfficesCodesSpecificationIsUsed_thenReturnApplicationsWithMatchingOfficeCodes() {
    // Arrange
    final String officeCode1 = UUID.randomUUID().toString();
    final String officeCode2 = UUID.randomUUID().toString();
    final ApplicationEntity applicationWithFirstOfficeCode =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeCode(officeCode1));
    final ApplicationEntity applicationWithSecondOfficeCode =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeCode(officeCode2));
    final ApplicationEntity applicationWithUnauthorizedOfficeCode =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerOfficeCode(UUID.randomUUID().toString()));
    applicationRepository.saveAndFlush(applicationWithFirstOfficeCode);
    applicationRepository.saveAndFlush(applicationWithSecondOfficeCode);
    applicationRepository.saveAndFlush(applicationWithUnauthorizedOfficeCode);
    clearCache();

    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.filterByProviderOfficesCodes(List.of(officeCode1, officeCode2));

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).hasSize(2);
    assertThat(applications)
        .extracting(ApplicationEntity::getProviderOfficeCode)
        .containsExactlyInAnyOrder(officeCode1, officeCode2);
  }

  @Test
  void whenProviderContractInformationSpecificationIsUsed_thenReturnMatchingApplications() {
    // Arrange
    final String providerFirmCode = "123456";
    final String officeCode = UUID.randomUUID().toString();
    final ApplicationEntity matchingApplication =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(providerFirmCode)
                    .providerOfficeCode(officeCode));
    final ApplicationEntity differentFirmCodeApplication =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode("654321")
                    .providerOfficeCode(officeCode));
    final ApplicationEntity unauthorizedOfficeCodeApplication =
        ApplicationEntityGenerator.createWithoutId(
            builder ->
                builder
                    .withDefaultClientDetails()
                    .providerFirmCode(providerFirmCode)
                    .providerOfficeCode(UUID.randomUUID().toString()));
    applicationRepository.saveAndFlush(matchingApplication);
    applicationRepository.saveAndFlush(differentFirmCodeApplication);
    applicationRepository.saveAndFlush(unauthorizedOfficeCodeApplication);
    clearCache();

    final Specification<ApplicationEntity> specification =
        ApplicationSpecification.filterByProviderContractInformation(
            providerFirmCode, List.of(officeCode));

    // Act
    final List<ApplicationEntity> applications = applicationRepository.findAll(specification);

    // Assert
    assertThat(applications).hasSize(1);
    assertThat(applications.getFirst().getProviderFirmCode()).isEqualTo(providerFirmCode);
    assertThat(applications.getFirst().getProviderOfficeCode()).isEqualTo(officeCode);
  }
}
