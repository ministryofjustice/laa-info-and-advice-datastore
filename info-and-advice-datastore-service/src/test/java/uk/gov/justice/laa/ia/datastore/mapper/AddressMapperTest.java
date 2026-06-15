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
            .addressLine3("Middle of London")
            .addressLine4("Near Big Ben")
            .townOrCity("London")
            .county("Greater London")
            .country("GB")
            .postCode("SW1A 2AA")
            .build();

    var mappedModel = sut.toAddress(addressEntity);

    assertEquals(addressEntity.getAddressLine1(), mappedModel.getAddressLine1());
    assertEquals(addressEntity.getAddressLine2(), mappedModel.getAddressLine2());
    assertEquals(addressEntity.getAddressLine3(), mappedModel.getAddressLine3());
    assertEquals(addressEntity.getAddressLine4(), mappedModel.getAddressLine4());
    assertEquals(addressEntity.getTownOrCity(), mappedModel.getTownOrCity());
    assertEquals(addressEntity.getCounty(), mappedModel.getCounty());
    assertEquals(addressEntity.getCountry(), mappedModel.getCountry());
    assertEquals(addressEntity.getPostCode(), mappedModel.getPostCode());
    assertEquals(addressEntity.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(addressEntity.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
  }

  @Test
  void toAddress_whenOptionalFieldsAreNull_thenShouldMapNull() {
    AddressEntity entity = createAddress().build();
    Address mappedModel = sut.toAddress(entity);
    assertNull(mappedModel.getAddressLine2());
    assertNull(mappedModel.getAddressLine3());
    assertNull(mappedModel.getAddressLine4());
    assertNull(mappedModel.getTownOrCity());
    assertNull(mappedModel.getCounty());
    assertNull(mappedModel.getCountry());
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

    assertEquals(cmd.getAddressLine1(), address.getAddressLine1());
    assertEquals(cmd.getAddressLine2(), address.getAddressLine2());
    assertEquals(cmd.getAddressLine3(), address.getAddressLine3());
    assertEquals(cmd.getAddressLine4(), address.getAddressLine4());
    assertEquals(cmd.getTownOrCity(), address.getTownOrCity());
    assertEquals(cmd.getPostCode(), address.getPostCode());
    assertEquals(cmd.getCounty(), address.getCounty());
    assertEquals(cmd.getCountry(), address.getCountry());
  }

  private AddressEntity.AddressEntityBuilder createAddress() {
    return AddressEntity.builder()
        .id(UUID.randomUUID())
        .addressLine1("10 Downing Street")
        .createdAt(Instant.now())
        .modifiedAt(Instant.now());
  }
}
