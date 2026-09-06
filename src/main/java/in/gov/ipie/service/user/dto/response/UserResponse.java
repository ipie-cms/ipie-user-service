package in.gov.ipie.service.user.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import in.gov.ipie.service.user.domain.AccountCategory;
import in.gov.ipie.service.user.domain.NotificationChannel;
import in.gov.ipie.service.user.domain.RegistrationStatus;
import in.gov.ipie.service.user.domain.UserStatus;

public record UserResponse(
        String id,

        // The user's Keycloak subject. Exposed because role administration is keyed on it:
        // ipie-iam-service's assign/revoke endpoints need both this and the id above, and this is
        // the only place the pairing is known. Null until registration completes.
        String keycloakUserId,

        String username,
        String email,
        String fullName,
        String phoneNumber,
        UserStatus status,
        RegistrationStatus registrationStatus,
        String organisationId,
        Set<NotificationChannel> notificationChannels,
        AccountCategory category,
        String addressLine1,
        String addressLine2,
        String country,
        String state,
        String city,
        String pin,
        String identityProofTypeId,
        /** Last four digits only - the rest is not stored (Aadhaar Act s.29). */
        String identityProofNumberLast4,
        /** Every professional role held, each with its own credential. */
        List<ProfessionalRoleResponse> professionalRoles,
        boolean emailVerified,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    /**
     * Defensive copy of the roles list, and the accessor hands one back too.
     *
     * <p>A record's generated accessor returns the field itself, so a caller holding the list it
     * passed in can still mutate what this record reports afterwards - the reason SpotBugs flags
     * EI_EXPOSE_REP on records carrying a collection. Copying on both sides makes the value actually
     * immutable. Null becomes an empty list rather than staying null, because "no roles" and "not
     * supplied" mean the same thing to every caller here.
     */
    public UserResponse {
        professionalRoles = professionalRoles == null ? List.of() : List.copyOf(professionalRoles);
    }

    @Override
    public List<ProfessionalRoleResponse> professionalRoles() {
        return List.copyOf(professionalRoles);
    }
}
