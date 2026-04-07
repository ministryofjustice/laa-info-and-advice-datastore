package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.model.Address;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class AddressMapperTest {
  @InjectMocks private final AddressMapper sut = new AddressMapperImpl();
  @Spy private final DateTimeMapper dateTimeMapper = new DateTimeMapperImpl();

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

  private AddressEntity.AddressEntityBuilder createAddress() {
    return AddressEntity.builder()
        .id(UUID.randomUUID())
        .clientHasHomeAddress(true)
        .createdAt(Instant.now())
        .modifiedAt(Instant.now());
  }
}
