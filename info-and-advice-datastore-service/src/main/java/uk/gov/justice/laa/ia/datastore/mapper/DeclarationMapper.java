package uk.gov.justice.laa.ia.datastore.mapper;

import org.mapstruct.Mapper;
import uk.gov.justice.laa.ia.datastore.entity.DeclarationEntity;
import uk.gov.justice.laa.ia.datastore.model.DeclarationResponse;

/** The mapper between Declaration and DeclarationEntity. */
@Mapper(
    componentModel = "spring",
    uses = {DateTimeMapper.class})
public interface DeclarationMapper {
  DeclarationResponse toDeclarationResponse(DeclarationEntity entity);
}
