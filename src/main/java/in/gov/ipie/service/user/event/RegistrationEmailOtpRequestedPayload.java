package in.gov.ipie.service.user.event;

import java.util.UUID;

/** Payload for {@link UserEventType#REGISTRATION_EMAIL_OTP_REQUESTED}. */
public record RegistrationEmailOtpRequestedPayload(UUID userId, String email, String code) {
}
