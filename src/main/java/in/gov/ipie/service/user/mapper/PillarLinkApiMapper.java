package in.gov.ipie.service.user.mapper;

import org.mapstruct.Mapper;

import in.gov.ipie.service.user.dto.response.PillarLinkResponse;
import in.gov.ipie.service.user.domain.PillarLink;

/** MapStruct mapping between the pillar-link API DTOs and domain model (master standards doc, 5.2). */
@Mapper(componentModel = "spring")
public interface PillarLinkApiMapper {

    PillarLinkResponse toResponse(PillarLink link);
}
