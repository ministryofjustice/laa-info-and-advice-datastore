package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.model.DeclarationCommand;
import uk.gov.justice.laa.ia.datastore.model.DeclarationResponse;

/** The mapper between Declaration and DeclarationEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class DeclarationMapper {

  @Autowired protected UserContext userContext;

  public abstract DeclarationResponse toDeclarationResponse(DeclarationEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "clientDeclarationStatus", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  public abstract DeclarationEntity toDeclarationEntity(DeclarationCommand command);
}
