package in.gov.ipie.service.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(max = 200)
        String fullName,

        @Pattern(regexp = "^[+0-9 ()-]{0,20}$", message = "must be a valid phone number")
        String phoneNumber,

        /**
         * The reason for this update, for the audit trail. Optional here (no
         * {@code @NotBlank}/{@code @NotNull}) - a human-facing UI making this call is expected to
         * require it before submitting, but nothing else calling this endpoint is forced to
         * supply one (see {@code Auditable}'s Javadoc).
         */
        @Size(max = 500)
        String comment) {
}

