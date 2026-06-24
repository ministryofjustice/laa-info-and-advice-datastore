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
    uses = {DateTimeMapper.class, ClientDetailsMapper.class, DeclarationMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ApplicationMapper {
  /** Maps an {@link ApplicationEntity} to an {@link ApplicationEntity}. */
  @Mapping(source = "meansAssessmentId", target = "meansAssessmentStatus")
  @Mapping(source = "clientDetails.id", target = "individualLegalAidNumber")
  @Mapping(source = "clientDetails", target = "client")
  @Mapping(source = "declaration", target = "declaration")
  @Mapping(source = "evidence", target = "evidence")
  ApplicationResponse toApplication(ApplicationEntity entity);

  /** Maps an {@link StartCaseCommand} to an {@link ApplicationEntity}. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(target = "providerFirmId", ignore = true)
  @Mapping(target = "providerOfficeId", ignore = true)
  @Mapping(target = "applicationState", ignore = true)
  @Mapping(target = "referenceNumber", ignore = true)
  @Mapping(target = "meansAssessmentId", ignore = true)
  @Mapping(target = "evidence", ignore = true)
  @Mapping(target = "declaration", ignore = true)
  @Mapping(target = "reasonForReapplication", ignore = true)
  @Mapping(target = "meansAssessmentRequired", ignore = true)
  @Mapping(target = "typeOfNonMeans", ignore = true)
  @Mapping(target = "ecfFlag", ignore = true)
  @Mapping(target = "contribution", ignore = true)
  @Mapping(target = "determinationId", ignore = true)
  @Mapping(source = "client", target = "clientDetails")
  ApplicationEntity toApplicationEntity(StartCaseCommand cmd);
}
