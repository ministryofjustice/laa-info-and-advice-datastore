package uk.gov.justice.laa.ia.datastore.client.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;

class NullableModelCompatibilityTest {

  private final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JsonNullableModule());

  @Test
  void shouldRetainCreateClientModelTypesAndRepresentScopingNullability() throws Exception {
    assertThat(UpdateClientDetailsCommand.class.getMethod("getNiNumber").getReturnType())
        .isEqualTo(String.class);
    assertThat(UpdateClientDetailsCommand.class.getMethod("getAddress").getReturnType())
        .isEqualTo(UpdateAddressCommand.class);
    assertThat(UpdateAddressCommand.class.getMethod("getAddressLine2").getReturnType())
        .isEqualTo(String.class);

    UpdateScopingDataCommand omitted =
        objectMapper.readValue("{\"eTag\":0}", UpdateScopingDataCommand.class);
    UpdateScopingDataCommand cleared =
        objectMapper.readValue(
            "{\"eTag\":0,\"scopingQuestions\":null}", UpdateScopingDataCommand.class);
    UpdateScopingDataCommand supplied =
        objectMapper.readValue(
            "{\"eTag\":0,\"scopingQuestions\":{\"priorLegalAid\":\"same_matter\"}}",
            UpdateScopingDataCommand.class);

    assertThat(omitted.getScopingQuestions_JsonNullable().isPresent()).isFalse();
    assertThat(cleared.getScopingQuestions_JsonNullable().isPresent()).isTrue();
    assertThat(cleared.getScopingQuestions()).isNull();
    assertThat(supplied.getScopingQuestions_JsonNullable().get())
        .containsEntry("priorLegalAid", "same_matter");

    String clearedJson = objectMapper.writeValueAsString(cleared);
    assertThat(objectMapper.readTree(clearedJson).get("scopingQuestions").isNull()).isTrue();
    assertThat(objectMapper.writeValueAsString(omitted)).doesNotContain("scopingQuestions");

    String suppliedJson = objectMapper.writeValueAsString(supplied);
    assertThat(objectMapper.readTree(suppliedJson).get("scopingQuestions").get("priorLegalAid"))
        .isEqualTo(objectMapper.getNodeFactory().textNode("same_matter"));
    assertThat(suppliedJson).doesNotContain("present", "value");
  }
}
