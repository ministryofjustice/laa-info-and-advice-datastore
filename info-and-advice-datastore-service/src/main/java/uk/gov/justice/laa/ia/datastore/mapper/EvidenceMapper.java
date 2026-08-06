package uk.gov.justice.laa.ia.datastore.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.model.EvidenceResponse;
import uk.gov.justice.laa.ia.datastore.model.UpdateEvidenceCommand;

/** The mapper between EvidenceEntity and evidence-related API models. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class, JsonNodeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class EvidenceMapper {

  @Autowired protected ObjectMapper objectMapper;

  @Mapping(target = "evidenceId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(
      target = "incomeEvidenceChecklist",
      expression = "java(objectMapper.valueToTree(command.getIncomeEvidenceChecklist()))")
  @Mapping(
      target = "expenditureCapitalEvidenceChecklist",
      expression =
          "java(objectMapper.valueToTree(command.getExpenditureCapitalEvidenceChecklist()))")
  public abstract EvidenceEntity toEvidenceEntity(UpdateEvidenceCommand command);

  public abstract EvidenceResponse toEvidenceResponse(EvidenceEntity entity);
}
