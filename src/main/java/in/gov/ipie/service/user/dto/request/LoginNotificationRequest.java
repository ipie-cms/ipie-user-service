package in.gov.ipie.service.user.dto.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record LoginNotificationRequest(

        @NotNull
        UUID keycloakUserId,

        @NotNull
        Instant occurredAt,

        String sourceIp) {
}
