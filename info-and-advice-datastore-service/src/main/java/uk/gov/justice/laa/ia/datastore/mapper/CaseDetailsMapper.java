package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.Mapper;
import uk.gov.justice.laa.ia.datastore.entity.CaseDetailsEntity;
import uk.gov.justice.laa.ia.datastore.model.CaseDetailsResponse;

/** The mapper between CaseDetails and CaseDetailsEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class})
public interface CaseDetailsMapper {
  CaseDetailsResponse toCaseDetailsResponse(CaseDetailsEntity entity);
}
