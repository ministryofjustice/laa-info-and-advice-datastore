package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.context.UserContext;
import uk.gov.justice.laa.ia.datastore.entity.ApplicationEntity;
import uk.gov.justice.laa.ia.datastore.model.ApplicationResponse;
import uk.gov.justice.laa.ia.datastore.model.ApplicationSummary;
import uk.gov.justice.laa.ia.datastore.model.StartApplicationCommand;
import uk.gov.justice.laa.ia.datastore.model.UpdateApplicationCommand;

/** The mapper between Application and ApplicationEntity. */
@Mapper(
    componentModel = "spring",
    uses = {
      DateTimeMapper.class,
      ClientDetailsMapper.class,
      DeclarationMapper.class,
      EligibilityMapper.class,
      EvidenceMapper.class,
      JsonNodeMapper.class
    },
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class ApplicationMapper {
  @Autowired protected UserContext userContext;

  /** Maps an {@link ApplicationEntity} to an {@link ApplicationSummary} for list views. */
  @Mapping(source = "clientDetails.firstName", target = "clientFirstName")
  @Mapping(source = "clientDetails.lastName", target = "clientLastName")
  public abstract ApplicationSummary toApplicationSummary(ApplicationEntity entity);

  /** Maps an {@link ApplicationEntity} to an {@link ApplicationResponse}. */
  @Mapping(source = "clientDetails.id", target = "individualLegalAidNumber")
  @Mapping(source = "clientDetails", target = "client")
  @Mapping(source = "declaration", target = "declaration")
  @Mapping(source = "evidence", target = "evidence")
  @Mapping(source = "mostRecentEligibilityResult", target = "eligibilityResult")
  @Mapping(source = "etag", target = "eTag")
  public abstract ApplicationResponse toApplication(ApplicationEntity entity);

  /** Maps an {@link StartApplicationCommand} to an {@link ApplicationEntity}. */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "providerFirmCode", expression = "java(userContext.getProviderFirmCode())")
  @Mapping(target = "applicationState", ignore = true)
  @Mapping(target = "referenceNumber", ignore = true)
  @Mapping(target = "laaReference", ignore = true)
  @Mapping(target = "scopingQuestions", ignore = true)
  @Mapping(target = "isMeansTested", ignore = true)
  @Mapping(target = "ufn", ignore = true)
  @Mapping(target = "dataRetentionEventUuid", ignore = true)
  @Mapping(target = "dataRetentionDate", ignore = true)
  @Mapping(target = "evidence", ignore = true)
  @Mapping(target = "declaration", ignore = true)
  @Mapping(target = "reasonForReapplication", ignore = true)
  @Mapping(target = "meansAssessmentRequired", ignore = true)
  @Mapping(target = "typeOfNonMeans", ignore = true)
  @Mapping(target = "ecfFlag", ignore = true)
  @Mapping(target = "contribution", ignore = true)
  @Mapping(target = "determinationId", ignore = true)
  @Mapping(source = "client", target = "clientDetails")
  @Mapping(target = "eligibilityResults", ignore = true)
  public abstract ApplicationEntity toApplicationEntity(StartApplicationCommand cmd);

  /**
   * Updates an {@link ApplicationEntity} in place from an {@link UpdateApplicationCommand}, leaving
   * fields that are not set on the command unchanged.
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "etag", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", expression = "java(userContext.getCurrentUser())")
  @Mapping(target = "providerFirmCode", ignore = true)
  @Mapping(target = "providerOfficeCode", ignore = true)
  @Mapping(target = "evidence", ignore = true)
  @Mapping(target = "declaration", ignore = true)
  @Mapping(target = "clientDetails", ignore = true)
  @Mapping(target = "referenceNumber", ignore = true)
  @Mapping(target = "laaReference", ignore = true)
  @Mapping(target = "scopingQuestions", ignore = true)
  @Mapping(target = "isMeansTested", ignore = true)
  @Mapping(target = "ufn", ignore = true)
  @Mapping(target = "dataRetentionEventUuid", ignore = true)
  @Mapping(target = "dataRetentionDate", ignore = true)
  @Mapping(target = "reasonForReapplication", ignore = true)
  @Mapping(target = "meansAssessmentRequired", ignore = true)
  @Mapping(target = "typeOfNonMeans", ignore = true)
  @Mapping(target = "ecfFlag", ignore = true)
  @Mapping(target = "contribution", ignore = true)
  @Mapping(target = "applicationType", ignore = true)
  @Mapping(target = "determinationId", ignore = true)
  @Mapping(target = "eligibilityResults", ignore = true)
  public abstract void updateApplicationEntity(
      UpdateApplicationCommand cmd, @MappingTarget ApplicationEntity entity);
}
