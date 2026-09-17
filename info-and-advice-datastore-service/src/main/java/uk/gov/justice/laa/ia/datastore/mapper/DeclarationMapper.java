package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.DeclarationResponse;
import uk.gov.justice.laa.ia.datastore.model.PatchDeclarationData;

/** The mapper between Declaration and DeclarationEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class DeclarationMapper {

  @Autowired protected UserContext userContext;

  public abstract DeclarationResponse toDeclarationResponse(DeclarationEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "clientDeclarationStatus", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "dateSigned", source = "dateSigned")
  public abstract DeclarationEntity toDeclarationEntity(DeclarationCommand command);

  /**
   * Updates a {@link DeclarationEntity} in place from a {@link PatchDeclarationData}, leaving
   * fields that are not set on the command unchanged. Used by the generic application PATCH
   * endpoint, where concurrency control is handled via the parent command's eTag rather than one on
   * this nested payload.
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "clientDeclarationStatus", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  public abstract void patchDeclarationEntity(
      PatchDeclarationData cmd, @MappingTarget DeclarationEntity entity);
}
