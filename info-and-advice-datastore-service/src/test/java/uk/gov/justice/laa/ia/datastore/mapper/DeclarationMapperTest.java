package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.DeclarationResponse;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class DeclarationMapperTest {
  @Autowired private DeclarationMapper sut;
  @MockitoBean private UserContext userContext;

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

  @Test
  void toDeclarationEntity_shouldSetCreatedByAndModifiedBy() {
    when(userContext.getCurrentUser()).thenReturn("USERCONTEXT:SYSTEM");
    final DeclarationCommand cmd =
        DeclarationCommand.builder().declarationConfirmation(true).build();

    final DeclarationEntity mappedEntity = sut.toDeclarationEntity(cmd);

    assertEquals("USERCONTEXT:SYSTEM", mappedEntity.getCreatedBy());
    assertEquals("USERCONTEXT:SYSTEM", mappedEntity.getModifiedBy());
  }
}
