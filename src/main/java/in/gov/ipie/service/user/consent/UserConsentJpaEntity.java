package in.gov.ipie.service.user.consent;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One person's decision about one item, against one notice version.
 *
 * <p>Only {@code withdrawnAt} is ever mutated. A re-grant is a new row, so the table reads as a
 * history rather than a current state - see {@code V34}.
 */
@Entity
@Table(name = "user_consents")
public class UserConsentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notice_id", nullable = false)
    private UUID noticeId;

    @Column(name = "item", nullable = false, length = 50)
    private String item;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "source", nullable = false, length = 32)
    private String source;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected UserConsentJpaEntity() {
        // JPA
    }

    public UserConsentJpaEntity(UUID id, UUID userId, UUID noticeId, String item, Instant grantedAt, String source) {
        this.id = id;
        this.userId = userId;
        this.noticeId = noticeId;
        this.item = item;
        this.grantedAt = grantedAt;
        this.source = source;
    }

    /** Withdrawal per DPDP s.7 - the row stays, so the grant it records stays provable. */
    public void withdraw(Instant withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getNoticeId() {
        return noticeId;
    }

    public String getItem() {
        return item;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
