package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for the {@link ClientDetailsEntity}. */
@ExtendWith(MockitoExtension.class)
public class ClientDetailsEntityTests {

  @Test
  void givenClientDetailsEntityWithNoFixedAbode_thenHasFixedAddressReturnsFalse() {
    // Arrange
    final ClientDetailsEntity entity = ClientDetailsEntity.builder().noFixedAbode(true).build();

    // Act
    final boolean hasFixedAddress = entity.hasFixedAddress();

    // Assert
    assertThat(hasFixedAddress).isFalse();
  }

  @Test
  void givenClientDetailsEntityWithFixedAbode_thenHasFixedAddressReturnsTrue() {
    // Arrange
    final ClientDetailsEntity entity = ClientDetailsEntity.builder().noFixedAbode(false).build();

    // Act
    final boolean hasFixedAddress = entity.hasFixedAddress();

    // Assert
    assertThat(hasFixedAddress).isTrue();
  }
}
