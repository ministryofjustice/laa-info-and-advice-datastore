package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.model.ClientDetails;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateClientDetailsCommand;

/** The mapper between ClientDetails and ClientDetailsEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class, AddressMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class ClientDetailsMapper {

  @Autowired protected UserContext userContext;

  /** Maps an {@link ClientDetailsEntity} to an {@link ClientDetails}. */
  @Mapping(source = "id", target = "individualLegalAidNumber")
  public abstract ClientDetails toClientDetails(ClientDetailsEntity entity);

  /** Maps an {@link CreateClientCommand} to an {@link ClientDetailsEntity}. */
  @Mapping(source = "nationalInsuranceNumber", target = "niNumber")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(source = "createAddressCommand", target = "address")
  @Mapping(source = "noFixedAbode", target = "noFixedAbode")
  @Mapping(target = "dataRetentionEventUuid", ignore = true)
  @Mapping(target = "dataRetentionDate", ignore = true)
  @Mapping(target = "createdBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  public abstract ClientDetailsEntity toClientDetailsEntity(
      CreateClientCommand createClientCommand);

  /**
   * Updates a {@link ClientDetailsEntity} in place from an {@link UpdateClientDetailsCommand},
   * leaving fields that are not set on the command unchanged. The nested address is merged via
   * {@link AddressMapper#updateAddressEntity}, which MapStruct selects automatically for this
   * property (creating a new {@link uk.gov.justice.laa.ia.datastore.entity.AddressEntity} first if
   * none exists yet).
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "dataRetentionEventUuid", ignore = true)
  @Mapping(target = "dataRetentionDate", ignore = true)
  public abstract void updateClientDetailsEntity(
      UpdateClientDetailsCommand cmd, @MappingTarget ClientDetailsEntity entity);
}
