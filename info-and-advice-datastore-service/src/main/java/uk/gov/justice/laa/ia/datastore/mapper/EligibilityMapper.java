package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityResultEntity;
import uk.gov.justice.laa.ia.datastore.model.EligibilityResultResponse;

/** The mapper between EligibilityResultEntity and EligibilityResultResponse. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EligibilityMapper {
  @Mapping(target = "eligibilityResult", source = "resultJson")
  EligibilityResultResponse toEligibilityResult(EligibilityResultEntity entity);
}
