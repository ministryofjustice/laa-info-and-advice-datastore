package uk.gov.justice.laa.ia.datastore.specification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApplicationSpecification#filterByProviderOfficesCodes(java.util.List)}.
 * These do not test the filtering behaviour and instead focus on the validation of the officeCodes
 * parameter.
 */
public class ProviderOfficeCodesSpecificationTests {

  @Test
  void givenNullOfficeCodes_whenFilterByProviderOfficesCodes_thenThrows() {
    assertThatThrownBy(() -> ApplicationSpecification.filterByProviderOfficesCodes(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("officeCodes must not be null or empty");
  }

  @Test
  void givenEmptyOfficeCodes_whenFilterByProviderOfficesCodes_thenThrows() {
    assertThatThrownBy(
            () -> ApplicationSpecification.filterByProviderOfficesCodes(Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("officeCodes must not be null or empty");
  }

  @Test
  void givenNullOfficeCodes_whenFilterByDefaultConstraints_thenThrows() {
    assertThatThrownBy(() -> ApplicationSpecification.filterByDefaultConstraints("123456", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("officeCodes must not be null or empty");
  }

  @Test
  void givenBlankProviderFirmCode_whenFilterByDefaultConstraints_thenThrows() {
    assertThatThrownBy(
            () ->
                ApplicationSpecification.filterByDefaultConstraints(
                    "", Collections.singletonList("office-1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerFirmCode must not be null or blank");
  }
}
