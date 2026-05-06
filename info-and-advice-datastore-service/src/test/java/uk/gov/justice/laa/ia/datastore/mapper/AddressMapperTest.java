package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.generator.CreateAddressCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.Address;
import uk.gov.justice.laa.ia.datastore.model.CreateAddressCommand;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class AddressMapperTest extends BaseMapperTest {
  private final AddressMapper sut;

  AddressMapperTest() {
    sut = addressMapper;
  }

  @Test
  void toAddress_shouldMapAllProperties() {
    var addressEntity =
        createAddress()
            .addressLine1("10 Downing Street")
            .addressLine2("Westminister")
            .townOrCity("London")
            .postCode("SW1A 2AA")
            .build();

    var mappedModel = sut.toAddress(addressEntity);

    assertEquals(addressEntity.isClientHasHomeAddress(), mappedModel.getClientHasHomeAddress());
    assertEquals(addressEntity.getAddressLine1(), mappedModel.getAddressLine1());
    assertEquals(addressEntity.getAddressLine2(), mappedModel.getAddressLine2());
    assertEquals(addressEntity.getTownOrCity(), mappedModel.getTownOrCity());
    assertEquals(addressEntity.getPostCode(), mappedModel.getPostCode());
    assertEquals(addressEntity.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(addressEntity.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
  }

  @Test
  void toAddress_whenOptionalFieldsAreNull_thenShouldMapNull() {
    AddressEntity entity = createAddress().build();
    Address mappedModel = sut.toAddress(entity);
    assertNull(mappedModel.getAddressLine1());
    assertNull(mappedModel.getAddressLine2());
    assertNull(mappedModel.getTownOrCity());
    assertNull(mappedModel.getPostCode());
  }

  @Test
  void toAddress_whenNull_thenReturnNull() {
    assertNull(sut.toAddress(null));
  }

  @Test
  void createAddressCommand_toAddressEntity_shouldMapAllProperties() {
    final CreateAddressCommand cmd = CreateAddressCommandGenerator.create(null);
    final AddressEntity address = sut.toAddressEntity(cmd);

    assertEquals(cmd.getLine1(), address.getAddressLine1());
    assertEquals(cmd.getLine2(), address.getAddressLine2());
    assertEquals(cmd.getCity(), address.getTownOrCity());
    assertEquals(cmd.getPostCode(), address.getPostCode());
  }

  private AddressEntity.AddressEntityBuilder createAddress() {
    return AddressEntity.builder()
        .id(UUID.randomUUID())
        .clientHasHomeAddress(true)
        .createdAt(Instant.now())
        .modifiedAt(Instant.now());
  }
}
