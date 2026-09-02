package in.gov.ipie.service.user.command;

import java.util.Set;

import in.gov.ipie.service.user.domain.NotificationChannel;

/** {@code notificationChannels} may be {@code null}/empty - defaults to {@code [EMAIL]} (see {@code User.defaultChannels}). */
public record CreateUserCommand(
        String username, String email, String fullName, String phoneNumber, Set<NotificationChannel> notificationChannels) {
}
