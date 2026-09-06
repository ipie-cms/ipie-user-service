package in.gov.ipie.service.user.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import in.gov.ipie.service.user.command.CreateOrganisationCommand;
import in.gov.ipie.service.user.command.UpdateOrganisationCommand;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.dto.request.CreateOrganisationRequest;
import in.gov.ipie.service.user.dto.request.UpdateOrganisationRequest;
import in.gov.ipie.service.user.dto.response.OrganisationResponse;

/** MapStruct mapping between the Organisation API's request/response DTOs and the application/domain model. */
@Mapper(componentModel = "spring")
public interface OrganisationApiMapper {

    CreateOrganisationCommand toCommand(CreateOrganisationRequest request);

    default UpdateOrganisationCommand toCommand(UUID organisationId, UpdateOrganisationRequest request) {
        return new UpdateOrganisationCommand(
                organisationId, request.name(), request.legalConstitution(), request.msme(), request.msmeType(),
                request.registeredAddress(), request.contactNumber(), request.contactEmail(), request.country(),
                request.state(), request.city(), request.pin(), request.district(), request.comment());
    }

    @Mapping(target = "createdAt", source = "auditMetadata.createdAt")
    @Mapping(target = "updatedAt", source = "auditMetadata.updatedAt")
    @Mapping(target = "version", source = "auditMetadata.version")
    OrganisationResponse toResponse(Organisation organisation);
}
