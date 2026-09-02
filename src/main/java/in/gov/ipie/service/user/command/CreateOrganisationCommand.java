package in.gov.ipie.service.user.command;

import in.gov.ipie.service.user.domain.LegalConstitution;
import in.gov.ipie.service.user.domain.OrganisationIdType;

public record CreateOrganisationCommand(
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
