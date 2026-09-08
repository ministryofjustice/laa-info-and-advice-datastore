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

  /**
   * Maps an {@link UpdateAddressCommand} onto an {@link AddressEntity} in place, leaving fields
   * that are not set on the command unchanged. Used both to populate a brand new {@link
   * AddressEntity} (pass in a freshly constructed instance) and to update an existing one - {@code
   * createdBy} is preserved if already set on the target, otherwise defaulted to the current user.
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(
      target = "createdBy",
      expression =
          "java(entity.getCreatedBy() != null ? entity.getCreatedBy() :"
              + " userContext.getCurrentUser())")
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  public abstract void updateAddressEntity(
      UpdateAddressCommand cmd, @MappingTarget AddressEntity entity);
}
