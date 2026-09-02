package in.gov.ipie.service.user.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import in.gov.ipie.common.utils.id.UuidV7Generator;

import in.gov.ipie.common.persistence.AuditableJpaEntity;
import in.gov.ipie.service.user.domain.PillarLinkRequestStatus;
import in.gov.ipie.service.user.domain.PillarType;

/**
 * JPA representation of a {@code PillarLinkRequest} - see {@code PillarLinkJpaEntity}'s
 * Javadoc for the convention. Standard audit + soft-delete columns are inherited from
 * {@link AuditableJpaEntity}.
 */
@Entity
@Table(name = "pillar_link_requests")
public class PillarLinkRequestJpaEntity extends AuditableJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pillar_type", nullable = false, length = 20)
    private PillarType pillarType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PillarLinkRequestStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected PillarLinkRequestJpaEntity() {
        // required by JPA
    }

    public PillarLinkRequestJpaEntity(
            UUID userId, PillarType pillarType, PillarLinkRequestStatus status, Instant expiresAt) {
        this.userId = userId;
        this.pillarType = pillarType;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public PillarType getPillarType() {
        return pillarType;
    }

    public PillarLinkRequestStatus getStatus() {
        return status;
    }

    public void setStatus(PillarLinkRequestStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
