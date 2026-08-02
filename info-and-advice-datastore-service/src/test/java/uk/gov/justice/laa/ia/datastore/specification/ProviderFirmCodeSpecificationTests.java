package uk.gov.justice.laa.ia.datastore.specification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApplicationSpecification#filterByFirmCode(String)}. These do not test the
 * filtering behaviour and instead focus on the validation of the firmCode parameter.
 */
public class FirmCodeSpecificationTests {

  @Test
  void givenNullFirmCode_whenFilterByFirmCode_thenThrows() {
    assertThatThrownBy(() -> ApplicationSpecification.filterByFirmCode(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("firmCode must not be null or blank");
  }

  @Test
  void givenBlankFirmCode_whenFilterByFirmCode_thenThrows() {
    assertThatThrownBy(() -> ApplicationSpecification.filterByFirmCode(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("firmCode must not be null or blank");
  }
}
