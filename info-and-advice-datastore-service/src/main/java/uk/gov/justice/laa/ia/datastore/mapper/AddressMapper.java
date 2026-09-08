package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.AddressEntity;
import uk.gov.justice.laa.ia.datastore.model.Address;
import uk.gov.justice.laa.ia.datastore.model.CreateAddressCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateAddressCommand;

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
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "createdBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  public abstract AddressEntity toAddressEntity(CreateAddressCommand cmd);

  /** Maps an {@link UpdateAddressCommand} to a new {@link AddressEntity}. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "createdBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  public abstract AddressEntity toAddressEntity(UpdateAddressCommand cmd);

  /**
   * Updates an {@link AddressEntity} in place from an {@link UpdateAddressCommand}, leaving fields
   * that are not set on the command unchanged.
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  public abstract void updateAddressEntity(
      UpdateAddressCommand cmd, @MappingTarget AddressEntity entity);
}
