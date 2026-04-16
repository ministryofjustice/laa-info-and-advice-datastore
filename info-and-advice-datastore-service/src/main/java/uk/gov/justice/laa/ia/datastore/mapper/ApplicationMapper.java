package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;

/** The mapper between Application and ApplicationEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class})
public interface ApplicationMapper {
  /** Maps an {@link ApplicationEntity} to an {@link Application}. */
  @Mapping(source = "id", target = "referenceNumber")
  @Mapping(source = "eligibilityResultId", target = "eligibilityResult")
  @Mapping(source = "meansAssessmentStatusId", target = "meansAssessmentStatus")
  @Mapping(source = "evidenceStatusId", target = "evidenceStatus")
  @Mapping(source = "clientDeclarationStatusId", target = "clientDeclarationStatus")
  ApplicationResponse toApplication(ApplicationEntity entity);
}
