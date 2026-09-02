package in.gov.ipie.service.user.dto.request;

import jakarta.validation.constraints.Size;

import in.gov.ipie.service.user.domain.LegalConstitution;
import in.gov.ipie.service.user.domain.OrganisationIdType;

/**
 * All-optional mirror of {@code CreateOrganisationRequest} - no {@code @NotBlank}/{@code @NotNull},
 * since a registration draft may capture a brand-new Entity's details incrementally. Shared
 * between {@link SaveRegistrationDraftRequest} and {@link CompleteRegistrationRequest}.
 */
public record EntityDraftDetails(

        @Size(max = 200)
        String name,

        LegalConstitution legalConstitution,

        OrganisationIdType idType,

        @Size(max = 50)
        String idValue,

        boolean msme,

        @Size(max = 50)
        String msmeType,

        @Size(max = 500)
        String registeredAddress,

        @Size(max = 20)
        String contactNumber,

        @Size(max = 254)
        String contactEmail,

        @Size(max = 100)
        String country,

        @Size(max = 100)
        String state,

        @Size(max = 100)
        String city,

        @Size(max = 10)
        String pin,

        @Size(max = 100)
        String district) {
}
