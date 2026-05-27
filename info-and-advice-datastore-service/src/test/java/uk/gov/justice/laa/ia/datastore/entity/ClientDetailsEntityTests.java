package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;

/** Unit tests for the {@link ClientDetailsEntity}. */
@ExtendWith(MockitoExtension.class)
public class ClientDetailsEntityTests {

  @Test
  void givenClientDetailsEntityWithNoAddress_thenHasFixedAddressReturnsFalse() {
    // Arrange
    final ClientDetailsEntity entity = new ClientDetailsEntity();

    // Act
    final boolean hasFixedAddress = entity.hasFixedAddress();

    // Assert
    assertThat(hasFixedAddress).isFalse();
  }

  @Test
  void givenClientDetailsEntityWithAddress_thenHasFixedAddressReturnsTrue() {
    // Arrange
    final ClientDetailsEntity entity =
        ClientDetailsEntity.builder().address(AddressEntityGenerator.createWithId(null)).build();

    // Act
    final boolean hasFixedAddress = entity.hasFixedAddress();

    // Assert
    assertThat(hasFixedAddress).isTrue();
  }
}
