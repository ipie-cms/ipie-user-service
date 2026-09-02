package in.gov.ipie.service.user.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import in.gov.ipie.common.core.model.AuditMetadata;

/**
 * Short-lived row backing the initiate -&gt; browser redirect -&gt; callback handshake for {@link
 * PillarLinkService#initiateLink}/{@code completeLinkCallback} - analogous in spirit to
 * {@code User#verificationToken}/{@code verificationTokenExpiresAt}.
 */
@Getter
@Builder(toBuilder = true)
public final class PillarLinkRequest {

    private final UUID id;
    private final UUID userId;
    private final PillarType pillarType;
    private PillarLinkRequestStatus status;
    private final Instant expiresAt;
    private final AuditMetadata auditMetadata;

    public static PillarLinkRequest createNew(UUID userId, PillarType pillarType, Instant expiresAt) {
        return PillarLinkRequest.builder()
                .userId(userId)
                .pillarType(pillarType)
                .status(PillarLinkRequestStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isPendingAndNotExpired() {
        return status == PillarLinkRequestStatus.PENDING && expiresAt.isAfter(Instant.now());
    }

    public void complete() {
        this.status = PillarLinkRequestStatus.COMPLETED;
    }
}
