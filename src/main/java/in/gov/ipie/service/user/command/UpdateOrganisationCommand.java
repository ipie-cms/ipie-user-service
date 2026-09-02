package in.gov.ipie.service.user.command;

import java.util.UUID;

import in.gov.ipie.service.user.domain.LegalConstitution;

public record UpdateOrganisationCommand(
        UUID organisationId,
        String name,
        LegalConstitution legalConstitution,
        boolean msme,
        String msmeType,
        String registeredAddress,
        String contactNumber,
        String contactEmail,
        String country,
        String state,
        String city,
        String pin,
        String district,
        String comment) {
}
