package in.gov.ipie.service.user.dto.request;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import in.gov.ipie.service.user.domain.NotificationChannel;

public record CreateUserRequest(

        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "must contain only letters, digits, dot, underscore or hyphen")
        String username,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(max = 200)
        String fullName,

        @Pattern(regexp = "^[+0-9 ()-]{0,20}$", message = "must be a valid phone number")
        String phoneNumber,

        /** Optional - omit to default to {@code [EMAIL]} (see {@code User.defaultChannels}). */
        Set<NotificationChannel> notificationChannels) {
}
