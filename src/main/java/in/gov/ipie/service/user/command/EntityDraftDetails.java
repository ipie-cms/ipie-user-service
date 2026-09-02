package in.gov.ipie.service.user.command;

import in.gov.ipie.service.user.domain.LegalConstitution;
import in.gov.ipie.service.user.domain.OrganisationIdType;

/**
 * The Entity Details a registration wizard has captured for a brand-new (not-yet-existing) Entity
 * - shared between {@link SaveRegistrationDraftCommand} and {@link CompleteRegistrationCommand} so
 * the shape isn't duplicated across both.
 */
public record EntityDraftDetails(
        String name,
        LegalConstitution legalConstitution,
        OrganisationIdType idType,
        String idValue,
        boolean msme,
        String msmeType,
        String registeredAddress,
        String contactNumber,
        String contactEmail,
        String country,
        String state,
        String city,
        String pin,
        String district) {
}
