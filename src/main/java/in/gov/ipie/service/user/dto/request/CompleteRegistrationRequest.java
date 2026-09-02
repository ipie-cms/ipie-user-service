package in.gov.ipie.service.user.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import in.gov.ipie.service.user.domain.AccountCategory;

/**
 * Step 2 ("SUBMIT FOR VERIFICATION") - grew from just {@code fullName}+{@code password} to carry
 * the full registration wizard payload. The rich fields below stay optional at this validation
 * layer (like {@link SaveRegistrationDraftRequest}'s) - only {@code fullName} is a hard requirement
 * here; {@code UserServiceImpl#completeRegistration}'s own business rule additionally requires the
 * email OTP to have been confirmed first (see {@code EmailNotVerifiedException}).
 *
 * <p><b>No password field, deliberately.</b> This service does not handle credentials at all
 * (ARCHITECTURE_WORKING_PLAN.md, §4.1.1) - it owns the person, not the account. Provisioning is
 * asynchronous and the Keycloak account is created without credentials, so a password submitted here
 * could only be held until that account existed, which would mean storing or queueing a plaintext
 * credential. The registrant chooses theirs afterwards, against ipie-iam-service, through the
 * one-time link emailed to them.
 */
public record CompleteRegistrationRequest(

        @NotBlank
        @Size(max = 200)
        String fullName,

        AccountCategory category,

        @Size(max = 500)
        String addressLine1,

        @Size(max = 500)
        String addressLine2,

        @Size(max = 100)
        String country,

        @Size(max = 100)
        String state,

        @Size(max = 100)
        String city,

        @Size(max = 10)
        String pin,

        String identityProofTypeId,

        @Size(max = 50)
        String identityProofNumber,

        /**
         * Every professional role claimed. The FRS allows several to be selected, each carrying its
         * own identification type and value.
         */
        @Valid
        List<ProfessionalRoleEntry> professionalRoles,

        /** Set when the wizard picked an already-registered Entity from search - takes precedence over {@link #entity}. */
        String organisationId,

        /** Set when the wizard is registering a brand-new Entity. */
        EntityDraftDetails entity) {

    /**
     * Defensive copy of the roles list, and the accessor hands one back too.
     *
     * <p>A record's generated accessor returns the field itself, so a caller holding the list it
     * passed in can still mutate what this record reports afterwards - the reason SpotBugs flags
     * EI_EXPOSE_REP on records carrying a collection. Copying on both sides makes the value actually
     * immutable. Null becomes an empty list rather than staying null, because "no roles" and "not
     * supplied" mean the same thing to every caller here.
     */
    public CompleteRegistrationRequest {
        professionalRoles = professionalRoles == null ? List.of() : List.copyOf(professionalRoles);
    }

    @Override
    public List<ProfessionalRoleEntry> professionalRoles() {
        return List.copyOf(professionalRoles);
    }
}
