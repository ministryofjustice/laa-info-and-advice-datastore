package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.model.ClientDetails;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;

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
}
