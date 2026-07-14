package uk.gov.justice.laa.ia.datastore.specification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApplicationSpecification#filterByProviderFirmId(UUID)}. These do not test
 * the filtering behaviour and instead focus on the validation of the providerFirmId parameter.
 */
public class ProviderFirmIdSpecificationTests {

  @Test
  void givenNullProviderFirmId_whenFilterByProviderFirmId_thenThrows() {
    assertThatThrownBy(() -> ApplicationSpecification.filterByProviderFirmId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerFirmId must not be null or empty UUID");
  }

  @Test
  void givenEmptyProviderFirmId_whenFilterByProviderFirmId_thenThrows() {
    assertThatThrownBy(
            () ->
                ApplicationSpecification.filterByProviderFirmId(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("providerFirmId must not be null or empty UUID");
  }
}
