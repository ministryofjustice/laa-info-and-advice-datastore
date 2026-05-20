package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;
import uk.gov.justice.laa.ia.datastore.generator.CreateAddressCommandGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CreateClientCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;
import uk.gov.justice.laa.ia.datastore.model.Individual;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class IndividualMapperTest extends BaseMapperTest {
  private final IndividualMapper sut;

  IndividualMapperTest() {
    sut = individualMapper;
  }

  @Test
  void toIndividual_shouldMapAllProperties() {
    IndividualEntity entity = createIndividual().niNumber("AD123456Q").build();

    Individual mappedModel = sut.toIndividual(entity);
    assertEquals(entity.getId(), mappedModel.getIndividualLegalAidNumber());
    assertEquals(entity.getFullName(), mappedModel.getFullName());
    assertEquals(entity.getDateOfBirth(), mappedModel.getDateOfBirth());
    assertEquals(entity.getNiNumber(), mappedModel.getNiNumber());
    assertEquals(entity.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(entity.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
  }

  @Test
  void toIndividual_whenOptionalFieldsAreNull_thenShouldMapNull() {
    IndividualEntity entity = createIndividual().build();
    Individual mappedModel = sut.toIndividual(entity);
    assertNull(mappedModel.getNiNumber());
  }

  @Test
  void toIndividual_whenNull_thenReturnNull() {
    assertNull(sut.toIndividual(null));
  }

  @Test
  void createClientCommand_toIndividual_shouldMapProperties() {
    final CreateClientCommand cmd =
        CreateClientCommandGenerator.create(
            builder -> {
              builder.createAddressCommand(CreateAddressCommandGenerator.create(null));
            });
    final IndividualEntity mappedModel = sut.toIndividualEntity(cmd);

    assertEquals(cmd.getNationalInsuranceNumber(), mappedModel.getNiNumber());
    assertEquals(cmd.getFullName(), mappedModel.getFullName());
    assertEquals(cmd.getDateOfBirth(), mappedModel.getDateOfBirth());
    assertNotNull(mappedModel.getAddress());
  }

  private IndividualEntity.IndividualEntityBuilder createIndividual() {
    return IndividualEntity.builder()
        .id(UUID.randomUUID())
        .fullName("Joe Bloggs")
        .dateOfBirth(LocalDate.of(1990, 01, 01))
        .createdAt(Instant.now())
        .modifiedAt(Instant.now());
  }
}
