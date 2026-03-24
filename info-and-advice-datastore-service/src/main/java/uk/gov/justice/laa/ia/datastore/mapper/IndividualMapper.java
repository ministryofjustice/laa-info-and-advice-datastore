package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.IndividualEntity;
import uk.gov.justice.laa.ia.datastore.model.Individual;

/** The mapper between Individual and IndividualEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class})
public interface IndividualMapper {

  /** Maps an {@link IndividualEntity} to an {@link Individual}. */
  @Mapping(source = "id", target = "individualLegalAidNumber")
  Individual toIndividual(IndividualEntity entity);
}
