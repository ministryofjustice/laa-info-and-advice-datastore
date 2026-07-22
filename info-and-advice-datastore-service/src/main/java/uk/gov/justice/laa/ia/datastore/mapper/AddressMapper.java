package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.model.Address;
import uk.gov.justice.laa.ia.datastore.model.CreateAddressCommand;

/** The mapper between Address and AddressEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class})
public abstract class AddressMapper {
  @Autowired protected UserContext userContext;

  /** Maps an {@link AddressEntity} to an {@link Address}. */
  public abstract Address toAddress(AddressEntity entity);

  /** Maps a {@link CreateAddressCommand} to an {@link AddressEntity}. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "createdBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  public abstract AddressEntity toAddressEntity(CreateAddressCommand cmd);
}
