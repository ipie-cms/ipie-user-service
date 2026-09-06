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
import in.gov.ipie.service.user.domain.PillarType;

/**
 * JPA representation of a {@code PillarLink}. Never returned from an API and never
 * referenced outside the infrastructure layer - {@code PillarLinkPersistenceMapper} converts
 * to/from the domain model at the repository boundary (same convention as {@code UserJpaEntity}).
 * Standard audit + soft-delete columns are inherited from {@link AuditableJpaEntity}.
 */
@Entity
@Table(name = "pillar_links")
public class PillarLinkJpaEntity extends AuditableJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pillar_type", nullable = false, length = 20)
    private PillarType pillarType;

    @Column(name = "external_pillar_id", nullable = false, length = 100)
    private String externalPillarId;

    @Column(name = "external_username", nullable = false, length = 100)
    private String externalUsername;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    protected PillarLinkJpaEntity() {
        // required by JPA
    }

    public PillarLinkJpaEntity(
            UUID userId, PillarType pillarType, String externalPillarId, String externalUsername, Instant linkedAt) {
        this.userId = userId;
        this.pillarType = pillarType;
        this.externalPillarId = externalPillarId;
        this.externalUsername = externalUsername;
        this.linkedAt = linkedAt;
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

    public String getExternalPillarId() {
        return externalPillarId;
    }

    public String getExternalUsername() {
        return externalUsername;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
