package uk.gov.justice.laa.ia.datastore.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.generator.DeclarationEntityGenerator;
import uk.gov.justice.laa.ia.datastore.model.DeclarationResponse;

/** Tests for the mapper behaviour. */
@ExtendWith(MockitoExtension.class)
public class DeclarationMapperTest {
  @InjectMocks private final DeclarationMapper sut = new DeclarationMapperImpl();
  @Spy private final DateTimeMapper dateTimeMapper = new DateTimeMapperImpl();

  @Test
  void toDeclarationResponse_shouldMappAllProperties() {
    final DeclarationEntity declaration =
        DeclarationEntityGenerator.createWithId(UUID.randomUUID(), null);

    final DeclarationResponse mappedModel = sut.toDeclarationResponse(declaration);

    assertEquals(declaration.getId(), mappedModel.getId());
    assertEquals(declaration.getReferenceNumber(), mappedModel.getReferenceNumber());
    assertEquals(
        declaration.getClientDeclarationStatus(), mappedModel.getClientDeclarationStatus());
    assertEquals(declaration.isDeclarationStatement(), mappedModel.getDeclarationStatement());
    assertEquals(declaration.getCreatedAt(), mappedModel.getCreatedAt().toInstant());
    assertEquals(declaration.getCreatedBy(), mappedModel.getCreatedBy());
    assertEquals(declaration.getModifiedAt(), mappedModel.getModifiedAt().toInstant());
    assertEquals(declaration.getModifiedBy(), mappedModel.getModifiedBy());
  }
}
