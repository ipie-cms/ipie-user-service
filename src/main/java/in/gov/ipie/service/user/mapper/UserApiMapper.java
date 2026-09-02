package in.gov.ipie.service.user.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import in.gov.ipie.service.user.dto.request.CompleteRegistrationRequest;
import in.gov.ipie.service.user.dto.request.ConfirmEmailOtpRequest;
import in.gov.ipie.service.user.dto.request.CreateRegistrationRequest;
import in.gov.ipie.service.user.dto.request.CreateUserRequest;
import in.gov.ipie.service.user.dto.request.SaveRegistrationDraftRequest;
import in.gov.ipie.service.user.dto.request.UpdateUserRequest;
import in.gov.ipie.service.user.dto.response.RegistrationResponse;
import in.gov.ipie.service.user.dto.response.UserResponse;
import in.gov.ipie.service.user.command.CompleteRegistrationCommand;
import in.gov.ipie.service.user.command.ConfirmEmailOtpCommand;
import in.gov.ipie.service.user.command.CreateRegistrationCommand;
import in.gov.ipie.service.user.command.CreateUserCommand;
import in.gov.ipie.service.user.command.EntityDraftDetails;
import in.gov.ipie.service.user.command.SaveRegistrationDraftCommand;
import in.gov.ipie.service.user.command.UpdateUserCommand;
import in.gov.ipie.service.user.domain.User;
import java.util.List;
import in.gov.ipie.service.user.domain.ProfessionalRoleHolding;
import in.gov.ipie.service.user.dto.request.ProfessionalRoleEntry;

/** MapStruct mapping between the API's request/response DTOs and the application/domain model (master standards doc, 5.2). */
@Mapper(componentModel = "spring")
public interface UserApiMapper {

    CreateUserCommand toCommand(CreateUserRequest request);

    default UpdateUserCommand toCommand(UUID userId, UpdateUserRequest request) {
        return new UpdateUserCommand(userId, request.email(), request.fullName(), request.phoneNumber(), request.comment());
    }

    CreateRegistrationCommand toCommand(CreateRegistrationRequest request);

    default CompleteRegistrationCommand toCommand(UUID registrationId, CompleteRegistrationRequest request) {
        return new CompleteRegistrationCommand(
                registrationId, request.fullName(), request.category(), request.addressLine1(),
                request.addressLine2(), request.country(), request.state(), request.city(), request.pin(),
                parseUuid(request.identityProofTypeId()), request.identityProofNumber(),
                toHoldings(request.professionalRoles()),
                parseUuid(request.organisationId()), toCommand(request.entity()));
    }

    default SaveRegistrationDraftCommand toCommand(UUID registrationId, SaveRegistrationDraftRequest request) {
        return new SaveRegistrationDraftCommand(
                registrationId, request.fullName(), request.category(), request.addressLine1(), request.addressLine2(),
                request.country(), request.state(), request.city(), request.pin(), parseUuid(request.identityProofTypeId()),
                request.identityProofNumber(), toHoldings(request.professionalRoles()),
                parseUuid(request.organisationId()), toCommand(request.entity()));
    }

    default ConfirmEmailOtpCommand toCommand(UUID registrationId, ConfirmEmailOtpRequest request) {
        return new ConfirmEmailOtpCommand(registrationId, request.code());
    }

    private static EntityDraftDetails toCommand(in.gov.ipie.service.user.dto.request.EntityDraftDetails entity) {
        if (entity == null) {
            return null;
        }
        return new EntityDraftDetails(
                entity.name(), entity.legalConstitution(), entity.idType(), entity.idValue(), entity.msme(),
                entity.msmeType(), entity.registeredAddress(), entity.contactNumber(), entity.contactEmail(),
                entity.country(), entity.state(), entity.city(), entity.pin(), entity.district());
    }

    private static UUID parseUuid(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    @Mapping(target = "createdAt", source = "auditMetadata.createdAt")
    @Mapping(target = "updatedAt", source = "auditMetadata.updatedAt")
    @Mapping(target = "version", source = "auditMetadata.version")
    @Mapping(target = "emailVerified", expression = "java(user.getEmailVerifiedAt() != null)")
    UserResponse toResponse(User user);
    // Property lookup ("auditMetadata", "id", "username", ...) works because User exposes
    // JavaBean getters - see the comment on User's accessor methods.

    @Mapping(target = "registrationId", source = "id")
    RegistrationResponse toRegistrationResponse(User user);

    /**
     * Request entries to domain holdings. An absent list is an empty one rather than null: every
     * caller downstream iterates it, and a null would make each of them decide separately.
     */
    private List<ProfessionalRoleHolding> toHoldings(List<ProfessionalRoleEntry> entries) {
        if (entries == null) {
            return List.of();
        }
        return entries.stream()
                .map(entry -> ProfessionalRoleHolding.builder()
                        .roleId(parseUuid(entry.roleId()))
                        .identificationTypeId(parseUuid(entry.identificationTypeId()))
                        .identificationValue(entry.identificationValue())
                        .legalRepresentativeTypeId(parseUuid(entry.legalRepresentativeTypeId()))
                        .build())
                .toList();
    }
}
