package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;
import uk.gov.justice.laa.ia.datastore.model.Individual;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class IndividualMapperTest {
  @InjectMocks private final IndividualMapper sut = new IndividualMapperImpl();
  @Spy private final DateTimeMapper dateTimeMapper = new DateTimeMapperImpl();

  @Test
  void toIndividual_shouldMapAllProperties() {
    IndividualEntity entity =
        createIndividual().lastNameAtBirth("Smith").niNumber("AD123456Q").build();

    Individual mappedModel = sut.toIndividual(entity);
    assertEquals(entity.getId(), mappedModel.getIndividualLegalAidNumber());
    assertEquals(entity.getFirstName(), mappedModel.getFirstName());
    assertEquals(entity.getLastName(), mappedModel.getLastName());
    assertEquals(entity.getLastNameAtBirth(), mappedModel.getLastNameAtBirth());
    assertEquals(entity.getDateOfBirth(), mappedModel.getDateOfBirth());
    assertEquals(entity.getNiNumber(), mappedModel.getNiNumber());
    assertEquals(entity.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(entity.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
  }

  @Test
  void toIndividual_whenOptionalFieldsAreNull_thenShouldMapNull() {
    IndividualEntity entity = createIndividual().build();
    Individual mappedModel = sut.toIndividual(entity);
    assertNull(mappedModel.getLastNameAtBirth());
    assertNull(mappedModel.getNiNumber());
  }

  @Test
  void toIndividual_whenNull_thenReturnNull() {
    assertNull(sut.toIndividual(null));
  }

  private IndividualEntity.IndividualEntityBuilder createIndividual() {
    return IndividualEntity.builder()
        .id(UUID.randomUUID())
        .firstName("Joe")
        .lastName("Bloggs")
        .dateOfBirth(LocalDate.of(1990, 01, 01))
        .createdAt(Instant.now())
        .modifiedAt(Instant.now());
  }
}
