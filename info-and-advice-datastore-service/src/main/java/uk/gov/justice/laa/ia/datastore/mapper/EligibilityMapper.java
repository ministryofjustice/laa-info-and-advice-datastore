package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.model.EligibilityResult;

/** The mapper between EligibilityResultEntity and EligibilityResult. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EligibilityMapper {
  @Mapping(target = "data", source = "data")
  @Mapping(target = "result", source = "resultJson")
  EligibilityResult toEligibilityResult(EligibilityResultEntity entity);
}
