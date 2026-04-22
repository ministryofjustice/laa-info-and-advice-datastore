package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CreateAddressCommandGenerator;
import uk.gov.justice.laa.ia.datastore.mapper.AddressMapper;
import uk.gov.justice.laa.ia.datastore.model.CreateAddressCommand;
import uk.gov.justice.laa.ia.datastore.repository.AddressRepository;

/** Unit tests for the {@link AddressService}. */
@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {
  @Mock private AddressRepository repo;
  @Mock private AddressMapper mapper;
  @InjectMocks private AddressService sut;

  @Test
  void shouldSaveAddress() {
    // Arrange
    final CreateAddressCommand cmd = CreateAddressCommandGenerator.create(null);
    final AddressEntity entity = AddressEntityGenerator.createWithId(null);
    when(mapper.toAddressEntity(cmd)).thenReturn(entity);
    when(repo.save(entity)).thenReturn(entity);

    // Act
    final UUID addressId = sut.createAddress(cmd);

    // Act
    verify(mapper, times(1)).toAddressEntity(cmd);
    verify(repo, times(1)).save(entity);
    assertThat(addressId).isEqualTo(entity.getId());
  }
}
