package in.gov.ipie.service.user.domain;

/**
 * A channel a user has opted into for account notifications (registration status, login alerts,
 * ...). {@code EMAIL} is the only channel with a real backing implementation in
 * ipie-communication-service today - {@code SMS} is a real, selectable option whose delivery is
 * currently a logging-only placeholder pending a vendor choice (see that service's
 * {@code LoggingSmsServiceImpl}).
 */
public enum NotificationChannel {
    EMAIL,
    SMS
}
