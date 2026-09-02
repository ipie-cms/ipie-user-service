package in.gov.ipie.service.user.dto.request;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import in.gov.ipie.service.user.domain.NotificationChannel;

public record CreateRegistrationRequest(

        @NotBlank
        @Pattern(regexp = "^[+0-9 ()-]{5,20}$", message = "must be a valid mobile number")
        String mobileNumber,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        /** Which channel(s) to notify about registration status and login alerts on - optional, defaults to {@code [EMAIL]}. */
        Set<NotificationChannel> notificationChannels) {
}
