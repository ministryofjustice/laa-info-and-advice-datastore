package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.Mapper;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.model.Address;

/** The mapper between Address and AddressEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class})
public interface AddressMapper {
  /** Maps an {@link AddressEntity} to an {@link Address}. */
  Address toAddress(AddressEntity entity);
}
