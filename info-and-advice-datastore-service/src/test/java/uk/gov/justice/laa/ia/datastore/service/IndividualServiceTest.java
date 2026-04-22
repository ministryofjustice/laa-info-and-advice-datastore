package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;
import uk.gov.justice.laa.ia.datastore.generator.CreateAddressCommandGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CreateClientCommandGenerator;
import uk.gov.justice.laa.ia.datastore.generator.IndividualEntityGenerator;
import uk.gov.justice.laa.ia.datastore.mapper.IndividualMapper;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;
import uk.gov.justice.laa.ia.datastore.model.Individual;
import uk.gov.justice.laa.ia.datastore.repository.IndividualRepository;

/** Unit tests for the {@link IndividualService}. */
@ExtendWith(MockitoExtension.class)
public class IndividualServiceTest {
  @Mock private IndividualRepository repo;
  @Mock private IndividualMapper mapper;
  @Mock private AddressService addressService;
  @InjectMocks private IndividualService sut;

  @Test
  void shouldGetAllIndividuals() {
    // Arrange
    IndividualEntity entity1 = IndividualEntity.builder().id(UUID.randomUUID()).build();
    IndividualEntity entity2 = IndividualEntity.builder().id(UUID.randomUUID()).build();
    Individual individual1 = Individual.builder().individualLegalAidNumber(entity1.getId()).build();
    Individual individual2 = Individual.builder().individualLegalAidNumber(entity2.getId()).build();
    when(repo.findAll()).thenReturn(List.of(entity1, entity2));
    when(mapper.toIndividual(entity1)).thenReturn(individual1);
    when(mapper.toIndividual(entity2)).thenReturn(individual2);

    // Act
    List<Individual> result = sut.getAllIndividuals();

    // Assert
    assertThat(result).hasSize(2).contains(individual1, individual2);
  }

  @Test
  void shouldGetIndividual() {
    // Arrange
    IndividualEntity entity = IndividualEntity.builder().id(UUID.randomUUID()).build();
    Individual individual = Individual.builder().individualLegalAidNumber(entity.getId()).build();
    when(repo.findById(entity.getId())).thenReturn(Optional.of(entity));
    when(mapper.toIndividual(entity)).thenReturn(individual);

    // Act
    Individual result = sut.getIndividual(entity.getId()).orElseThrow();

    // Assert
    assertThat(result).isEqualTo(individual);
  }

  @Test
  void shouldReturnEmptyOptional_whenIndividualDoesNotExist() {
    // Arrange
    when(repo.findById(any(UUID.class))).thenReturn(Optional.empty());

    // Act
    Optional<Individual> result = sut.getIndividual(UUID.randomUUID());

    // Assert
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldSavedIndividual() {
    testCreateClientBehaviour(CreateClientCommandGenerator.create(null));
  }

  @Test
  void givenIndividualWithAddress_whenSave_thenShouldSaveAddress() {
    final CreateClientCommand cmd =
        CreateClientCommandGenerator.create(
            builder -> {
              builder.createAddressCommand(CreateAddressCommandGenerator.create(null));
            });
    testCreateClientBehaviour(cmd);
    verify(addressService, times(1)).createAddress(cmd.getCreateAddressCommand());
  }

  private void testCreateClientBehaviour(CreateClientCommand cmd) {
    final IndividualEntity entity = IndividualEntityGenerator.createWithId(null);
    when(mapper.toIndividualEntity(cmd)).thenReturn(entity);
    when(repo.save(entity)).thenReturn(entity);

    sut.handleCreateCommand(cmd);

    verify(mapper, times(1)).toIndividualEntity(cmd);
    verify(repo, times(1)).save(entity);
  }
}
