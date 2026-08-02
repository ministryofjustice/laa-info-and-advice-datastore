package uk.gov.justice.laa.ia.datastore.specification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApplicationSpecification#filterByProviderFirmCode(String)}. These do not
 * test the filtering behaviour and instead focus on the validation of the providerFirmCode
 * parameter.
 */
public class ProviderFirmCodeSpecificationTests {

  @Test
  void givenNullProviderFirmCode_whenFilterByProviderFirmCode_thenThrows() {
    assertThatThrownBy(() -> ApplicationSpecification.filterByProviderFirmCode(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerFirmCode must not be null or blank");
  }

  @Test
  void givenBlankProviderFirmCode_whenFilterByProviderFirmCode_thenThrows() {
    assertThatThrownBy(() -> ApplicationSpecification.filterByProviderFirmCode(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerFirmCode must not be null or blank");
  }
}
