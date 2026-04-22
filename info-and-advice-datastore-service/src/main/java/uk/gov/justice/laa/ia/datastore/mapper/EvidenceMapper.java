package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import uk.gov.justice.laa.ia.datastore.entity.EvidenceEntity;
import uk.gov.justice.laa.ia.datastore.model.EvidenceResponse;

/** The mapper between Evidence and EvidenceEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class},
    injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface EvidenceMapper {
  EvidenceResponse toEvidenceResponse(EvidenceEntity entity);
}
