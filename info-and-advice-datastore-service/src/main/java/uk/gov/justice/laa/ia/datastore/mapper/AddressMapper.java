package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.model.Address;
import uk.gov.justice.laa.ia.datastore.model.CreateAddressCommand;

/** The mapper between Address and AddressEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface AddressMapper {
  /** Maps an {@link AddressEntity} to an {@link Address}. */
  Address toAddress(AddressEntity entity);

  /** Maps a {@link CreateAddressCommand} to an {@link AddressEntity}. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  AddressEntity toAddressEntity(CreateAddressCommand cmd);
}
