package in.gov.ipie.service.user.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import in.gov.ipie.service.user.domain.NotificationChannel;

public record UpdateNotificationChannelsRequest(

        @NotEmpty
        Set<NotificationChannel> notificationChannels,

        /**
         * The reason for this change, for the audit trail. Optional here (no
         * {@code @NotBlank}/{@code @NotNull}) - a human-facing UI making this call is expected to
         * require it before submitting, but nothing else calling this endpoint is forced to
         * supply one (see {@code Auditable}'s Javadoc).
         */
        @Size(max = 500)
        String comment) {
}
