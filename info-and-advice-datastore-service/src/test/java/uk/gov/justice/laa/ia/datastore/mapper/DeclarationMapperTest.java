package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.DeclarationResponse;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class DeclarationMapperTest extends BaseMapperTest {
  @InjectMocks private final DeclarationMapper sut;

  DeclarationMapperTest() {
    sut = new DeclarationMapperImpl(dtMapper);
  }

  @Test
  void toDeclarationResponse_shouldMapAllProperties() {
    final DeclarationEntity declaration = DeclarationEntityGenerator.createWithId(null);

    final DeclarationResponse mappedModel = sut.toDeclarationResponse(declaration);

    assertEquals(declaration.getId(), mappedModel.getId());
    assertEquals(
        declaration.getClientDeclarationStatus(), mappedModel.getClientDeclarationStatus());
    assertEquals(declaration.isDeclarationConfirmation(), mappedModel.getDeclarationConfirmation());
    assertEquals(declaration.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(declaration.getCreatedBy(), mappedModel.getCreatedBy());
    assertEquals(declaration.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
    assertEquals(declaration.getModifiedBy(), mappedModel.getModifiedBy());
  }

  @Test
  void toDeclarationEntity_shouldMapAllProperties() {
    final DeclarationCommand cmd =
        DeclarationCommand.builder().declarationConfirmation(true).build();

    final DeclarationEntity mappedEntity = sut.toDeclarationEntity(cmd);

    assertEquals(cmd.getDeclarationConfirmation(), mappedEntity.isDeclarationConfirmation());
  }
}
