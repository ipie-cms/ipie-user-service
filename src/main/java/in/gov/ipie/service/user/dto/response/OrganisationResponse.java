package in.gov.ipie.service.user.dto.response;

import java.time.Instant;

import in.gov.ipie.service.user.domain.LegalConstitution;
import in.gov.ipie.service.user.domain.OrganisationIdType;

public record OrganisationResponse(
        String id,
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
        String district,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
