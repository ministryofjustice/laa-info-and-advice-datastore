package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.generator.CreateAddressCommandGenerator;
import uk.gov.justice.laa.ia.datastore.generator.CreateClientCommandGenerator;
import uk.gov.justice.laa.ia.datastore.model.ClientDetails;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class ClientDetailsMapperTest {
  @Autowired private ClientDetailsMapper sut;
  @MockitoBean private UserContext userContext;

  @Test
  void toClientDetails_shouldMapAllProperties() {
    ClientDetailsEntity entity =
        createClientDetails()
            .niNumber("AD123456Q")
            .address(
                uk.gov.justice.laa.ia.datastore.generator.AddressEntityGenerator.createWithId(null))
            .build();

    ClientDetails mappedModel = sut.toClientDetails(entity);
    assertEquals(entity.getId(), mappedModel.getIndividualLegalAidNumber());
    assertEquals(entity.getFirstName(), mappedModel.getFirstName());
    assertEquals(entity.getLastName(), mappedModel.getLastName());
    assertEquals(entity.getDateOfBirth(), mappedModel.getDateOfBirth());
    assertEquals(entity.getNiNumber(), mappedModel.getNiNumber());
    assertEquals(entity.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(entity.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
    assertNotNull(mappedModel.getAddress());
    assertEquals(entity.getAddress().getAddressLine1(), mappedModel.getAddress().getAddressLine1());
  }

  @Test
  void toClientDetails_whenOptionalFieldsAreNull_thenShouldMapNull() {
    ClientDetailsEntity entity = createClientDetails().build();
    ClientDetails mappedModel = sut.toClientDetails(entity);
    assertNull(mappedModel.getNiNumber());
  }

  @Test
  void toClientDetails_whenNull_thenReturnNull() {
    assertNull(sut.toClientDetails(null));
  }

  @Test
  void createClientCommand_toClientDetails_shouldMapProperties() {
    final CreateClientCommand cmd =
        CreateClientCommandGenerator.create(
            builder -> {
              builder.createAddressCommand(CreateAddressCommandGenerator.create(null));
            });
    final ClientDetailsEntity mappedModel = sut.toClientDetailsEntity(cmd);

    assertEquals(cmd.getNationalInsuranceNumber(), mappedModel.getNiNumber());
    assertEquals(cmd.getFirstName(), mappedModel.getFirstName());
    assertEquals(cmd.getLastName(), mappedModel.getLastName());
    assertEquals(cmd.getDateOfBirth(), mappedModel.getDateOfBirth());
    assertNotNull(mappedModel.getAddress());
  }

  @Test
  void createClientCommand_toClientDetails_shouldSetCreatedAndModifiedBy() {
    // Arrange
    when(userContext.getCurrentUser()).thenReturn("USERCONTEXT:SYSTEM");
    final CreateClientCommand cmd =
        CreateClientCommandGenerator.create(
            builder -> {
              builder.createAddressCommand(CreateAddressCommandGenerator.create(null));
            });

    // Act
    final ClientDetailsEntity mappedModel = sut.toClientDetailsEntity(cmd);

    // Assert
    assertEquals("USERCONTEXT:SYSTEM", mappedModel.getCreatedBy());
    assertEquals("USERCONTEXT:SYSTEM", mappedModel.getModifiedBy());
  }

  private static ClientDetailsEntity.ClientDetailsEntityBuilder createClientDetails() {
    return ClientDetailsEntity.builder()
        .id(UUID.randomUUID())
        .firstName("Joe")
        .lastName("Bloggs")
        .dateOfBirth(LocalDate.of(1990, 01, 01))
        .createdAt(Instant.now())
        .modifiedAt(Instant.now());
  }
}
