package uk.gov.justice.laa.ia.datastore.mapper;

import java.time.LocalDate;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapitools.jackson.nullable.JsonNullable;
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
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "clientDeclarationStatus", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "dateSigned", source = "dateSigned", qualifiedByName = "unwrapDate")
  public abstract DeclarationEntity toDeclarationEntity(DeclarationCommand command);

  @Named("unwrapDate")
  protected LocalDate unwrapDate(JsonNullable<LocalDate> value) {
    return value != null && value.isPresent() ? value.get() : null;
  }
}
