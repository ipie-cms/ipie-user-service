package in.gov.ipie.service.user.event;

import java.time.Instant;
import java.util.UUID;

import in.gov.ipie.service.user.domain.PillarType;

/**
 * Payload for {@link UserEventType#ACCOUNT_LINKED} - carries everything ipie-iam-service's
 * projection consumer needs to upsert a {@code pillar_resolution} row without a
 * cross-service call back to this service (ADR-001).
 */
public record AccountLinkedPayload(
        UUID userId,
        UUID keycloakUserId,
        PillarType pillarType,
        String externalPillarId,
        boolean verified,
        Instant linkedAt) {
}
