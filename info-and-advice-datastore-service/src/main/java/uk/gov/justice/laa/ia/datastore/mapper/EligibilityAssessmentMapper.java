package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.EligibilityAssessmentEntity;
import uk.gov.justice.laa.ia.datastore.model.EligibilityAssessment;
import uk.gov.justice.laa.ia.datastore.model.EligibilityResult;

/** The mapper between EligibilityAssessmentEntity and EligibilityAssessment. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class, JsonNodeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EligibilityAssessmentMapper {
  @Mapping(target = "data", source = "data")
  @Mapping(target = "result", source = "resultJson")
  EligibilityAssessment toEligibilityAssessment(EligibilityAssessmentEntity entity);

  @Mapping(target = "data", source = "data")
  @Mapping(target = "result", source = "resultJson")
  EligibilityResult toEligibilityResult(EligibilityAssessmentEntity entity);
}
