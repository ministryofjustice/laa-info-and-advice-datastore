package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.justice.laa.ia.datastore.entity.CaseDetailsEntity;
import uk.gov.justice.laa.ia.datastore.model.CaseDetailsResponse;
import uk.gov.justice.laa.ia.datastore.model.CreateCaseDetailsCommand;

/** The mapper between CaseDetails and CaseDetailsEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CaseDetailsMapper {
  CaseDetailsResponse toCaseDetailsResponse(CaseDetailsEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(source = "hasLegalHelpLastSixMonths", target = "hasSixMonthsLegalHelp")
  @Mapping(source = "isTypeNonMeansTested", target = "typeNonMeansTested")
  @Mapping(source = "requiresEcf", target = "requireEcf")
  CaseDetailsEntity toCaseDetailsEntity(CreateCaseDetailsCommand cmd);
}
