package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;
import uk.gov.justice.laa.ia.datastore.model.CreateClientCommand;
import uk.gov.justice.laa.ia.datastore.model.Individual;

/** The mapper between Individual and IndividualEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class, AddressMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface IndividualMapper {

  /** Maps an {@link IndividualEntity} to an {@link Individual}. */
  @Mapping(source = "id", target = "individualLegalAidNumber")
  Individual toIndividual(IndividualEntity entity);

  /** Maps an {@link CreateClientCommand} to an {@link IndividualEntity}. */
  @Mapping(source = "nationalInsuranceNumber", target = "niNumber")
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(source = "createAddressCommand", target = "address")
  IndividualEntity toIndividualEntity(CreateClientCommand createClientCommand);
}
