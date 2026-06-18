package uk.gov.justice.laa.ia.datastore.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ApplicationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.EligibilityResultEntityGenerator;
import uk.gov.justice.laa.ia.datastore.utils.BaseIntegrationTest;

/** Integration tests for the EligibilityResultRepository. */
@WithMockUser()
public class EligibilityResultRepositoryIntegrationTest extends BaseIntegrationTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void shouldSaveAndRetrieveEligibilityResult() {
    // Arrange: Create and save an application first (for the foreign key)
    ApplicationEntity application =
        ApplicationEntityGenerator.createWithoutId(
            builder -> {
              builder.clientDetails(
                  ClientDetailsEntityGenerator.createWithoutId(
                      clientDetailsBuilder -> {
                        clientDetailsBuilder.address(AddressEntityGenerator.createWithoutId(null));
                      }));
            });
    ApplicationEntity savedApplication = applicationRepository.saveAndFlush(application);

    // Arrange: Create eligibility result
    JsonNode resultJson =
        objectMapper.createObjectNode().put("status", "ELIGIBLE").put("score", 100);
    EligibilityResultEntity entity =
        EligibilityResultEntityGenerator.createWithoutId(
            builder -> {
              builder.applicationId(savedApplication.getId());
              builder.resultJson(resultJson);
            });

    // Act
    EligibilityResultEntity savedEntity = eligibilityResultRepository.saveAndFlush(entity);
    clearCache();

    // Assert
    EligibilityResultEntity retrievedEntity =
        eligibilityResultRepository.findById(savedEntity.getEligibilityResultId()).orElseThrow();

    assertThat(retrievedEntity.getEligibilityResultId()).isNotNull();
    assertThat(retrievedEntity.getApplicationId()).isEqualTo(savedApplication.getId());
    assertThat(retrievedEntity.getResultJson()).isEqualTo(resultJson);
    assertThat(retrievedEntity.getCreatedDate()).isNotNull();
  }
}
