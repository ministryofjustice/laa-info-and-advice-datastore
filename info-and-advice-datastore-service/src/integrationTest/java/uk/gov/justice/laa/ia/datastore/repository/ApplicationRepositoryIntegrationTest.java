package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import lombok.experimental.ExtensionMethod;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityBuilderExtensions;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the ApplicationRepository. */
@WithMockUser()
@ExtensionMethod(ApplicationEntityBuilderExtensions.class)
public class ApplicationRepositoryIntegrationTest extends BaseIntegrationTest {
  @Test
  void shouldGetApplication() {
    final ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.evidence(
                  EvidenceEntity.builder()
                      .evidenceExemptionCode("EXEMPT_01")
                      .createdBy("TEST_USER")
                      .modifiedBy("TEST_USER")
                      .build());
              builder.declaration(DeclarationEntityGenerator.createWithoutId(null));
            });
    final ApplicationEntity savedEntity = applicationRepository.saveAndFlush(entity);
    clearCache();
    final ApplicationEntity getEntity =
        applicationRepository.findById(savedEntity.getId()).orElseThrow();

    assertThat(getEntity)
        .usingRecursiveComparison()
        .ignoringFields("referenceNumber", "eligibilityResults")
        .ignoringFieldsMatchingRegexes(".*createdAt", ".*modifiedAt")
        .isEqualTo(entity);
    assertThat(getEntity.getCreatedAt()).isNotNull();
    assertThat(getEntity.getModifiedAt()).isNotNull();
    assertThat(getEntity.getReferenceNumber()).isNotNull();
    assertTrue(getEntity.getReferenceNumber().matches(referenceNumberRegex));
    assertThat(getEntity.getEligibilityResults()).isEmpty();
  }

  @Test
  void shouldSaveDeclarationWhenSavingApplication() {
    final ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails();
              builder.declaration(
                  DeclarationEntityGenerator.createWithoutId(
                      declarationBuilder -> declarationBuilder.declarationConfirmation(true)));
            });
    final ApplicationEntity savedEntity = applicationRepository.saveAndFlush(entity);
    clearCache();
    final ApplicationEntity getEntity =
        applicationRepository.findById(savedEntity.getId()).orElseThrow();

    assertThat(getEntity.getDeclaration().isDeclarationConfirmation()).isTrue();
  }

  @Test
  void shouldSaveEvidenceWhenSavingApplication() {
    final var evidence =
        EvidenceEntity.builder()
            .evidenceExemptionCode("EXEMPT_01")
            .createdBy("TEST_USER")
            .modifiedBy("TEST_USER")
            .build();
    final ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.withDefaultClientDetails().evidence(evidence);
            });
    final ApplicationEntity savedEntity = applicationRepository.saveAndFlush(entity);
    clearCache();
    final ApplicationEntity getEntity =
        applicationRepository.findById(savedEntity.getId()).orElseThrow();
    assertThat(getEntity.getEvidence().getEvidenceExemptionCode())
        .isEqualTo(evidence.getEvidenceExemptionCode());
  }

  @Test
  void shouldAllowMultipleApplicationsWithoutUfnForSameOfficeCode() {
    final String officeCode = UUID.randomUUID().toString();
    final ApplicationEntity first =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeCode(officeCode));
    final ApplicationEntity second =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeCode(officeCode));

    applicationRepository.saveAndFlush(first);

    assertThatCode(() -> applicationRepository.saveAndFlush(second)).doesNotThrowAnyException();
  }

  @Test
  void shouldAllowSameUfnForDifferentOfficeCodes() {
    final String ufn = "123456/1";
    final ApplicationEntity first =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().ufn(ufn));
    final ApplicationEntity second =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().ufn(ufn));

    applicationRepository.saveAndFlush(first);

    assertThatCode(() -> applicationRepository.saveAndFlush(second)).doesNotThrowAnyException();
  }

  @Test
  void shouldRejectDuplicateUfnForSameOfficeCode() {
    final String officeCode = UUID.randomUUID().toString();
    final String ufn = "123456/1";
    final ApplicationEntity first =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeCode(officeCode).ufn(ufn));
    final ApplicationEntity second =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeCode(officeCode).ufn(ufn));

    applicationRepository.saveAndFlush(first);

    assertThatThrownBy(() -> applicationRepository.saveAndFlush(second))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void existsByProviderOfficeCodeAndUfnAndIdNot_returnsFalse_whenOnlyMatchIsTheSameApplication() {
    final String officeCode = UUID.randomUUID().toString();
    final String ufn = "123456/1";
    final ApplicationEntity entity =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeCode(officeCode).ufn(ufn));
    final ApplicationEntity saved = applicationRepository.saveAndFlush(entity);

    assertThat(
            applicationRepository.existsByProviderOfficeCodeAndUfnAndIdNot(
                officeCode, ufn, saved.getId()))
        .isFalse();
  }

  @Test
  void existsByProviderOfficeCodeAndUfnAndIdNot_returnsTrue_whenAnotherApplicationHasSameUfn() {
    final String officeCode = UUID.randomUUID().toString();
    final String ufn = "123456/1";
    final ApplicationEntity existing =
        ApplicationEntityGenerator.createWithoutId(
            builder -> builder.withDefaultClientDetails().providerOfficeCode(officeCode).ufn(ufn));
    applicationRepository.saveAndFlush(existing);

    assertThat(
            applicationRepository.existsByProviderOfficeCodeAndUfnAndIdNot(
                officeCode, ufn, UUID.randomUUID()))
        .isTrue();
  }

  private final String referenceNumberRegex = "L-\\w{3}-\\w{3}";
}
