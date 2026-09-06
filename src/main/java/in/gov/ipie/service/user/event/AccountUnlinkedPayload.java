package in.gov.ipie.service.user.event;

import java.util.UUID;

import in.gov.ipie.service.user.domain.PillarType;

/**
 * Payload for {@link UserEventType#ACCOUNT_UNLINKED}. Defined alongside {@link AccountLinkedPayload}
 * so the projection consumer's sync mechanism is complete per ADR-001, even though no
 * user-facing "unlink" use case exists in {@code PillarLinkService} yet - see that
 * interface's Javadoc for the follow-up note.
 */
public record AccountUnlinkedPayload(UUID userId, PillarType pillarType, String externalPillarId) {
}
