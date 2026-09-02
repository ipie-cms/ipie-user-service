package in.gov.ipie.service.user.command;

import java.util.List;
import java.util.UUID;

import in.gov.ipie.service.user.domain.ProfessionalRoleHolding;
import in.gov.ipie.service.user.domain.AccountCategory;

/**
 * Carries no password: the account is provisioned without credentials and the user sets one later
 * through the link emailed to them - see {@code CompleteRegistrationRequest}.
 */
public record CompleteRegistrationCommand(
        UUID registrationId,
        String fullName,
        AccountCategory category,
        String addressLine1,
        String addressLine2,
        String country,
        String state,
        String city,
        String pin,
        UUID identityProofTypeId,
        String identityProofNumber,
        /** Every role claimed, each with its own credential - the FRS allows several. */
        List<ProfessionalRoleHolding> professionalRoles,
        UUID organisationId,
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
    public CompleteRegistrationCommand {
        professionalRoles = professionalRoles == null ? List.of() : List.copyOf(professionalRoles);
    }

    @Override
    public List<ProfessionalRoleHolding> professionalRoles() {
        return List.copyOf(professionalRoles);
    }
}
