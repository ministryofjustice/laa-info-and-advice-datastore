package uk.gov.justice.laa.ia.datastore.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.model.EvidenceResponse;
import uk.gov.justice.laa.ia.datastore.model.PatchEvidenceData;
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

  /**
   * Updates an {@link EvidenceEntity} in place from a {@link PatchEvidenceData}, leaving fields
   * that are not set on the command unchanged. Used by the generic application PATCH endpoint,
   * where concurrency control is handled via the parent command's eTag rather than one on this
   * nested payload.
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "evidenceId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "modifiedBy", ignore = true)
  @Mapping(
      target = "incomeEvidenceChecklist",
      expression =
          "java(command.getIncomeEvidenceChecklist() != null ?"
              + " objectMapper.valueToTree(command.getIncomeEvidenceChecklist()) :"
              + " entity.getIncomeEvidenceChecklist())")
  @Mapping(
      target = "expenditureCapitalEvidenceChecklist",
      expression =
          "java(command.getExpenditureCapitalEvidenceChecklist() != null ?"
              + " objectMapper.valueToTree(command.getExpenditureCapitalEvidenceChecklist()) :"
              + " entity.getExpenditureCapitalEvidenceChecklist())")
  public abstract void patchEvidenceEntity(
      PatchEvidenceData command, @MappingTarget EvidenceEntity entity);
}
