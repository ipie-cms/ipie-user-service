package in.gov.ipie.service.user.event;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import in.gov.ipie.service.user.domain.NotificationChannel;

/**
 * Payload for {@link UserEventType#USER_LOGGED_IN} - carries everything ipie-communication-service
 * needs to notify the account owner over their opted channel(s) without an extra callback into
 * this service.
 */
public record UserLoggedInPayload(
        UUID userId, String email, String mobileNumber, String fullName, Set<NotificationChannel> notificationChannels,
        Instant occurredAt, String sourceIp) {
}
