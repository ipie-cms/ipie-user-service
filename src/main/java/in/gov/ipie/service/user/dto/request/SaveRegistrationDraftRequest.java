package in.gov.ipie.service.user.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import in.gov.ipie.service.user.domain.AccountCategory;

/**
 * The registration wizard's "Save Draft" action - every field is optional (unlike {@link
 * CompleteRegistrationRequest}, which requires them at final submit), since a draft may be saved
 * from any step, partially filled in. Callable repeatedly; each call overwrites the previous draft
 * state with whatever the wizard currently holds (the frontend always sends its full in-progress
 * state, not a partial diff).
 */
public record SaveRegistrationDraftRequest(

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

        /** Set when the wizard is registering a brand-new Entity (Entity account type, "Create and Register New Entity Now"). */
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
    public SaveRegistrationDraftRequest {
        professionalRoles = professionalRoles == null ? List.of() : List.copyOf(professionalRoles);
    }

    @Override
    public List<ProfessionalRoleEntry> professionalRoles() {
        return List.copyOf(professionalRoles);
    }
}
