package in.gov.ipie.service.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import in.gov.ipie.service.user.domain.LegalConstitution;

public record UpdateOrganisationRequest(

        @NotBlank
        @Size(max = 200)
        String name,

        @NotNull
        LegalConstitution legalConstitution,

        boolean msme,

        @Size(max = 50)
        String msmeType,

        @Size(max = 500)
        String registeredAddress,

        @Size(max = 20)
        String contactNumber,

        @Email
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
        String district,

        /**
         * The reason for this update, for the audit trail. Optional here - a human-facing UI is
         * expected to require it before submitting (see {@code Auditable}'s Javadoc).
         */
        @Size(max = 500)
        String comment) {
}
