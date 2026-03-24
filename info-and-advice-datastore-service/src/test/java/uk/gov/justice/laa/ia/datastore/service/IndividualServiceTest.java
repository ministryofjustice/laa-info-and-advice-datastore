package uk.gov.justice.laa.ia.datastore.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;
import uk.gov.justice.laa.ia.datastore.mapper.IndividualMapper;
import uk.gov.justice.laa.ia.datastore.model.Individual;
import uk.gov.justice.laa.ia.datastore.repository.IndividualRepository;

/** Unit tests for the {@link IndividualService}. */
@ExtendWith(MockitoExtension.class)
public class IndividualServiceTest {
  @Mock private IndividualRepository repo;
  @Mock private IndividualMapper mapper;
  @InjectMocks private IndividualService sut;

  @Test
  void shouldGetAllItems() {
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
}
