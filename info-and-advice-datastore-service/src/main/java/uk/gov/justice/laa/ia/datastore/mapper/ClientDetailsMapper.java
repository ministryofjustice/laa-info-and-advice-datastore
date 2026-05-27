package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.ClientDetailsEntity;
import uk.gov.justice.laa.ia.datastore.model.ClientDetails;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;

/** The mapper between ClientDetails and ClientDetailsEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class, AddressMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ClientDetailsMapper {

  /** Maps an {@link ClientDetailsEntity} to an {@link ClientDetails}. */
  @Mapping(source = "id", target = "individualLegalAidNumber")
  ClientDetails toClientDetails(ClientDetailsEntity entity);

  /** Maps an {@link CreateClientCommand} to an {@link ClientDetailsEntity}. */
  @Mapping(source = "nationalInsuranceNumber", target = "niNumber")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(source = "createAddressCommand", target = "address")
  ClientDetailsEntity toClientDetailsEntity(CreateClientCommand createClientCommand);
}
