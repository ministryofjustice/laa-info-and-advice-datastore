package uk.gov.justice.laa.ia.datastore.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;

/** Unit tests for the {@link IndividualEntity}. */
@ExtendWith(MockitoExtension.class)
public class IndividualEntityTests {

  @Test
  void givenIndividualEntityWithNoAddress_thenHasFixedAddressReturnsFalse() {
    // Arrange
    final IndividualEntity entity = new IndividualEntity();

    // Act
    final boolean hasFixedAddress = entity.hasFixedAddress();

    // Assert
    assertThat(hasFixedAddress).isFalse();
  }

  @Test
  void givenIndividualEntityWithAddress_thenHasFixedAddressReturnsTrue() {
    // Arrange
    final IndividualEntity entity =
        IndividualEntity.builder().address(AddressEntityGenerator.createWithId(null)).build();

    // Act
    final boolean hasFixedAddress = entity.hasFixedAddress();

    // Assert
    assertThat(hasFixedAddress).isTrue();
  }
}
