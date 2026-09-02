package in.gov.ipie.service.user.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import in.gov.ipie.common.core.model.AuditMetadata;

/**
 * An authoritative record that an ipie user's account has been linked to their account at one
 * pillar (IBBI/NCLT/NCLAT/MCA). This service's own database is the source of
 * truth for the link (see this migration's header comment) - Keycloak never needs to be consulted
 * to answer "is this external identity linked, and to whom."
 */
@Getter
@Builder(toBuilder = true)
public final class PillarLink {

    private final UUID id;
    private final UUID userId;
    private final PillarType pillarType;
    private final String externalPillarId;
    private final String externalUsername;
    private final Instant linkedAt;
    private final AuditMetadata auditMetadata;

    public static PillarLink createNew(
            UUID userId, PillarType pillarType, String externalPillarId, String externalUsername) {
        return PillarLink.builder()
                .userId(userId)
                .pillarType(pillarType)
                .externalPillarId(externalPillarId)
                .externalUsername(externalUsername)
                .linkedAt(Instant.now())
                .build();
    }
}
