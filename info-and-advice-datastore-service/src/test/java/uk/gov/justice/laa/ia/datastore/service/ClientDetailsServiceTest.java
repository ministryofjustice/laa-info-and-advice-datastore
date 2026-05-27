package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.generator.ClientDetailsEntityGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CreateAddressCommandGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CreateClientCommandGenerator;
import uk.gov.justice.laa.ia.datastore.mapper.ClientDetailsMapper;
import uk.gov.justice.laa.ia.datastore.model.ClientDetails;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;
import uk.gov.justice.laa.ia.datastore.repository.ClientDetailsRepository;

/** Unit tests for the {@link ClientDetailsService}. */
@ExtendWith(MockitoExtension.class)
public class ClientDetailsServiceTest {
  @Mock private ClientDetailsRepository repo;
  @Mock private ClientDetailsMapper mapper;
  @Mock private AddressService addressService;
  @InjectMocks private ClientDetailsService sut;

  @Test
  void shouldGetClientDetails() {
    // Arrange
    ClientDetailsEntity entity = ClientDetailsEntity.builder().id(UUID.randomUUID()).build();
    ClientDetails clientDetails =
        ClientDetails.builder().individualLegalAidNumber(entity.getId()).build();
    when(repo.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(mapper.toClientDetails(entity)).thenReturn(clientDetails);

    // Act
    ClientDetails result = sut.getClientDetails(entity.getId()).orElseThrow();

    // Assert
    assertThat(result).isEqualTo(clientDetails);
  }

  @Test
  void shouldReturnEmptyOptional_whenClientDetailsDoesNotExist() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    Optional<ClientDetails> result = sut.getClientDetails(UUID.randomUUID());

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldSavedClientDetails() {
    testCreateClientBehaviour(CreateClientCommandGenerator.create(null));
  }

  @Test
  void givenClientDetailsWithAddress_whenSave_thenShouldSaveAddress() {
    final CreateClientCommand cmd =
        CreateClientCommandGenerator.create(
            builder -> {
              builder.createAddressCommand(CreateAddressCommandGenerator.create(null));
            });
    testCreateClientBehaviour(cmd);
    verify(addressService, times(1)).createAddress(cmd.getCreateAddressCommand());
  }

  private void testCreateClientBehaviour(CreateClientCommand cmd) {
    final ClientDetailsEntity entity = ClientDetailsEntityGenerator.createWithId(null);
    when(mapper.toClientDetailsEntity(cmd)).thenReturn(entity);
    when(repo.save(entity)).thenReturn(entity);

    sut.handleCreateCommand(cmd);

    verify(mapper, times(1)).toClientDetailsEntity(cmd);
    verify(repo, times(1)).save(entity);
  }
}
