package uk.gov.justice.laa.ia.datastore.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.justice.laa.ia.datastore.model.EditApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.PatchAddressData;
import uk.gov.justice.laa.ia.datastore.model.PatchClientDetailsData;
import uk.gov.justice.laa.ia.datastore.model.UpdateAddressCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateClientDetailsCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateScopingDataCommand;

class JsonNullablePatchContractTest {

  private final ObjectMapper objectMapper = new JacksonConfig().objectMapper();

  @Test
  void shouldDeserializeApplicationNullableFieldsAsUndefinedNullOrValue() throws Exception {
    final EditApplicationCommand omitted =
        objectMapper.readValue("{\"eTag\":0}", EditApplicationCommand.class);
    final EditApplicationCommand cleared =
        objectMapper.readValue(
            """
            {"eTag":0,"reasonForReapplication":null,"ecfFlag":null,"scopingQuestions":null}
            """,
            EditApplicationCommand.class);
    final EditApplicationCommand supplied =
        objectMapper.readValue(
            """
            {
              "eTag":0,
              "reasonForReapplication":"legacy",
              "ecfFlag":false,
              "scopingQuestions":{"priorLegalAid":"yes"}
            }
            """,
            EditApplicationCommand.class);

    assertUndefined(omitted.getReasonForReapplication());
    assertUndefined(omitted.getEcfFlag());
    assertUndefined(omitted.getScopingQuestions());
    assertNullValue(cleared.getReasonForReapplication());
    assertNullValue(cleared.getEcfFlag());
    assertNullValue(cleared.getScopingQuestions());
    assertThat(supplied.getReasonForReapplication().get()).isEqualTo("legacy");
    assertThat(supplied.getEcfFlag().get()).isFalse();
    assertThat(supplied.getScopingQuestions().get()).containsEntry("priorLegalAid", "yes");
  }

  @Test
  void shouldDeserializeClientAndAddressNullableFieldsAsUndefinedNullOrValue() throws Exception {
    final PatchClientDetailsData omittedClient =
        objectMapper.readValue("{}", PatchClientDetailsData.class);
    final PatchClientDetailsData clearedClient =
        objectMapper.readValue(
            "{\"niNumber\":null,\"address\":null}", PatchClientDetailsData.class);
    final PatchClientDetailsData suppliedClient =
        objectMapper.readValue(
            """
            {
                "niNumber":"QQ123456B",
                "address":{
                    "addressLine1":"1 Main Street",
                    "addressLine2":null,
                    "addressLine3":"Overseas line",
                    "addressLine4":null,
                    "townOrCity":"London",
                    "postCode":null,
                    "county":"Kent",
                    "country":"GB"
                }
            }
            """,
            PatchClientDetailsData.class);
    final PatchAddressData omittedAddress = objectMapper.readValue("{}", PatchAddressData.class);

    assertUndefined(omittedClient.getNiNumber());
    assertUndefined(omittedClient.getAddress());
    assertNullValue(clearedClient.getNiNumber());
    assertNullValue(clearedClient.getAddress());
    assertThat(suppliedClient.getNiNumber().get()).isEqualTo("QQ123456B");
    assertThat(suppliedClient.getAddress().get().getAddressLine1()).isEqualTo("1 Main Street");
    assertThat(suppliedClient.getAddress().get().getCountry()).isEqualTo("GB");

    final PatchAddressData address = suppliedClient.getAddress().get();
    assertUndefined(omittedAddress.getAddressLine2());
    assertUndefined(omittedAddress.getAddressLine3());
    assertUndefined(omittedAddress.getAddressLine4());
    assertUndefined(omittedAddress.getTownOrCity());
    assertUndefined(omittedAddress.getPostCode());
    assertUndefined(omittedAddress.getCounty());
    assertNullValue(address.getAddressLine2());
    assertThat(address.getAddressLine3().get()).isEqualTo("Overseas line");
    assertNullValue(address.getAddressLine4());
    assertThat(address.getTownOrCity().get()).isEqualTo("London");
    assertNullValue(address.getPostCode());
    assertThat(address.getCounty().get()).isEqualTo("Kent");
  }

  @Test
  void shouldDeserializeEveryPatchAddressFieldAsUndefinedNullOrValue() throws Exception {
    final PatchAddressData omitted = objectMapper.readValue("{}", PatchAddressData.class);
    final PatchAddressData cleared =
        objectMapper.readValue(
            """
            {
                "addressLine2": null,
                "addressLine3": null,
                "addressLine4": null,
                "townOrCity": null,
                "postCode": null,
                "county": null
            }
            """,
            PatchAddressData.class);
    final PatchAddressData supplied =
        objectMapper.readValue(
            """
            {
                "addressLine2": "Flat 2",
                "addressLine3": "Overseas line 3",
                "addressLine4": "Overseas line 4",
                "townOrCity": "London",
                "postCode": "SW1A 1AA",
                "county": "Kent"
            }
            """,
            PatchAddressData.class);

    assertUndefined(omitted.getAddressLine2());
    assertUndefined(omitted.getAddressLine3());
    assertUndefined(omitted.getAddressLine4());
    assertUndefined(omitted.getTownOrCity());
    assertUndefined(omitted.getPostCode());
    assertUndefined(omitted.getCounty());
    assertNullValue(cleared.getAddressLine2());
    assertNullValue(cleared.getAddressLine3());
    assertNullValue(cleared.getAddressLine4());
    assertNullValue(cleared.getTownOrCity());
    assertNullValue(cleared.getPostCode());
    assertNullValue(cleared.getCounty());
    assertThat(supplied.getAddressLine2().get()).isEqualTo("Flat 2");
    assertThat(supplied.getAddressLine3().get()).isEqualTo("Overseas line 3");
    assertThat(supplied.getAddressLine4().get()).isEqualTo("Overseas line 4");
    assertThat(supplied.getTownOrCity().get()).isEqualTo("London");
    assertThat(supplied.getPostCode().get()).isEqualTo("SW1A 1AA");
    assertThat(supplied.getCounty().get()).isEqualTo("Kent");
  }

  @Test
  void shouldDeserializeScopingCommandAsUndefinedNullOrValue() throws Exception {
    final UpdateScopingDataCommand omitted =
        objectMapper.readValue("{\"eTag\":0}", UpdateScopingDataCommand.class);
    final UpdateScopingDataCommand cleared =
        objectMapper.readValue(
            "{\"eTag\":0,\"scopingQuestions\":null}", UpdateScopingDataCommand.class);
    final UpdateScopingDataCommand supplied =
        objectMapper.readValue(
            "{\"eTag\":0,\"scopingQuestions\":{\"priorLegalAid\":\"yes\"}}",
            UpdateScopingDataCommand.class);

    assertUndefined(omitted.getScopingQuestions());
    assertNullValue(cleared.getScopingQuestions());
    assertThat(supplied.getScopingQuestions().get()).containsEntry("priorLegalAid", "yes");
  }

  @Test
  void shouldRejectExplicitNullForNonNullableJourneyFields() throws NoSuchMethodException {
    assertThat(PatchClientDetailsData.class.getMethod("getFirstName").getReturnType())
        .isEqualTo(String.class);
    assertThat(PatchClientDetailsData.class.getMethod("getLastName").getReturnType())
        .isEqualTo(String.class);
    assertThat(PatchClientDetailsData.class.getMethod("getDateOfBirth").getReturnType())
        .isEqualTo(java.time.LocalDate.class);
    assertThat(PatchClientDetailsData.class.getMethod("getNoFixedAbode").getReturnType())
        .isEqualTo(Boolean.class);
    assertThat(PatchAddressData.class.getMethod("getAddressLine1").getReturnType())
        .isEqualTo(String.class);
    assertThat(PatchAddressData.class.getMethod("getCountry").getReturnType())
        .isEqualTo(String.class);

    List<String> invalidBodies =
        List.of(
            "{\"clientDetails\":{\"firstName\":null}}",
            "{\"clientDetails\":{\"lastName\":null}}",
            "{\"clientDetails\":{\"dateOfBirth\":null}}",
            "{\"clientDetails\":{\"noFixedAbode\":null}}",
            "{\"clientDetails\":{\"address\":{\"addressLine1\":null}}}",
            "{\"clientDetails\":{\"address\":{\"country\":null}}}");

    invalidBodies.forEach(
        body ->
            assertThatThrownBy(() -> objectMapper.readValue(body, EditApplicationCommand.class))
                .isInstanceOf(JsonProcessingException.class));
  }

  @Test
  void shouldSerializeNullableCommandWithRequestJsonShape() {
    EditApplicationCommand command = new EditApplicationCommand(0L);
    command.setEcfFlag(JsonNullable.of(null));
    command.setScopingQuestions(JsonNullable.of(Map.of("priorLegalAid", "yes")));

    var json = objectMapper.valueToTree(command);

    assertThat(json.has("reasonForReapplication")).isFalse();
    assertThat(json.get("ecfFlag").isNull()).isTrue();
    assertThat(json.get("scopingQuestions").get("priorLegalAid").asText()).isEqualTo("yes");
    assertThat(json.toString()).doesNotContain("present", "value");
  }

  @Test
  void shouldKeepCreateClientRequestModelsOrdinaryAndScopeCommandNullable() throws Exception {
    assertThat(UpdateClientDetailsCommand.class.getMethod("getNiNumber").getReturnType())
        .isEqualTo(String.class);
    assertThat(UpdateClientDetailsCommand.class.getMethod("getAddress").getReturnType())
        .isEqualTo(UpdateAddressCommand.class);
    assertThat(UpdateAddressCommand.class.getMethod("getAddressLine2").getReturnType())
        .isEqualTo(String.class);
    assertThat(UpdateScopingDataCommand.class.getMethod("getScopingQuestions").getReturnType())
        .isEqualTo(JsonNullable.class);
  }

  private void assertUndefined(JsonNullable<?> value) {
    assertThat(value).isNotNull();
    assertThat(value.isPresent()).isFalse();
  }

  private void assertNullValue(JsonNullable<?> value) {
    assertThat(value.isPresent()).isTrue();
    assertThat(value.get()).isNull();
  }
}
