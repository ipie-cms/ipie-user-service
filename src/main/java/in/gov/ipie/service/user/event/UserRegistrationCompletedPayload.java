package in.gov.ipie.service.user.event;

import java.util.Set;
import java.util.UUID;

import in.gov.ipie.service.user.domain.NotificationChannel;

/**
 * Payload for {@link UserEventType#USER_REGISTRATION_COMPLETED} - carries everything
 * ipie-communication-service needs to render and send both emails this moment produces, without an
 * extra callback into this service.
 *
 * <p>{@code verificationToken} authorises a pillar admin to <b>approve</b> this registration
 * and belongs only in the mail to the configured admin address - never in the mail to {@code email}.
 * It cannot set a password: the registrant's set-password link is a separate token issued by
 * ipie-iam-service and delivered by its own event, so nothing here can take an account over.
 */
public record UserRegistrationCompletedPayload(
        UUID userId, String email, String fullName, String mobileNumber, String verificationToken,
        Set<NotificationChannel> notificationChannels) {
}
