package uk.gov.justice.laa.ia.datastore.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity;

/** Generator for EligibilityAssessmentEntity. */
@UtilityClass
public class EligibilityAssessmentEntityGenerator {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Create an EligibilityAssessmentEntity without an ID.
   *
   * @param customizer consumer to customize the entity
   * @return the entity
   */
  public static EligibilityAssessmentEntity createWithoutId(
      Consumer<EligibilityAssessmentEntity.EligibilityAssessmentEntityBuilder> customizer) {

    JsonNode dummyJson = objectMapper.createObjectNode().put("success", true);
    JsonNode dummyData = objectMapper.createObjectNode().put("client_age", "18-24");

    var builder =
        EligibilityAssessmentEntity.builder()
            .applicationId(UUID.randomUUID())
            .data(dummyData)
            .resultJson(dummyJson);

    if (customizer != null) {
      customizer.accept(builder);
    }

    return builder.build();
  }

  /**
   * Create a CFE v6 result trimmed from a CCQ-captured response. It keeps the shapes that must
   * survive a typed round-trip unchanged: explicit nulls, empty and filled arrays, omitted partner
   * sections and CFE's {@code 999999999999.0} unlimited sentinel.
   */
  @SneakyThrows
  public static JsonNode createResultJson() {
    return objectMapper.readTree(
        """
        {
          "version": "6",
          "timestamp": "2024-04-30T19:30:20.635Z",
          "success": true,
          "result_summary": {
            "overall_result": {
              "result": "ineligible",
              "capital_contribution": 0.0,
              "income_contribution": 0.0,
              "proceeding_types": [
                {
                  "ccms_code": "SE003",
                  "upper_threshold": 0.0,
                  "lower_threshold": 0.0,
                  "result": "ineligible",
                  "client_involvement_type": "A"
                }
              ]
            },
            "gross_income": {
              "total_gross_income": 2600.0,
              "combined_total_gross_income": 2600.0,
              "proceeding_types": [
                {
                  "ccms_code": "SE003",
                  "upper_threshold": 2657.0,
                  "lower_threshold": 0.0,
                  "result": "eligible",
                  "client_involvement_type": "A"
                }
              ]
            },
            "capital": {
              "total_capital": 0.0,
              "total_mortgage_allowance": 999999999999.0,
              "combined_assessed_capital": 0.0,
              "proceeding_types": []
            }
          },
          "assessment": {
            "client_reference_id": null,
            "submission_date": "2023-02-15",
            "level_of_help": "controlled",
            "applicant": {"involvement_type": "applicant", "employed": null},
            "gross_income": {
              "employment_income": [],
              "irregular_income": {
                "monthly_equivalents": {"student_loan": 0.0, "unspecified_source": 0.0}
              }
            },
            "capital": {
              "capital_items": {
                "liquid": [],
                "properties": {"main_home": null, "additional_properties": []}
              }
            },
            "remarks": {}
          }
        }
        """);
  }

  /** Create a basic EligibilityAssessmentEntity. */
  public static EligibilityAssessmentEntity createEligibilityAssessment(UUID applicationId) {
    return EligibilityAssessmentEntityGenerator.createWithoutId(builder -> {
      builder.applicationId(applicationId).createdAt(Instant.now()).createdBy("SYSTEM");
    });
  }
}
