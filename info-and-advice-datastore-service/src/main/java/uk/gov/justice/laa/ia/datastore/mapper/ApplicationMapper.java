package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.StartCaseCommand;

/** The mapper between Application and ApplicationEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class, IndividualMapper.class, CaseDetailsMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ApplicationMapper {
  /** Maps an {@link ApplicationEntity} to an {@link Application}. */
  @Mapping(source = "id", target = "referenceNumber")
  @Mapping(source = "eligibilityResultId", target = "eligibilityResult")
  @Mapping(source = "meansAssessmentStatusId", target = "meansAssessmentStatus")
  @Mapping(source = "evidenceStatusId", target = "evidenceStatus")
  @Mapping(source = "clientDeclarationStatusId", target = "clientDeclarationStatus")
  @Mapping(source = "individual.id", target = "individualLegalAidNumber")
  ApplicationResponse toApplication(ApplicationEntity entity);

  /** Maps an {@link StartCaseCommand} to an {@link ApplicationEntity}. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "providerFirmId", ignore = true)
  @Mapping(target = "providerOfficeId", ignore = true)
  @Mapping(source = "client", target = "individual")
  ApplicationEntity toApplicationEntity(StartCaseCommand cmd);
}
