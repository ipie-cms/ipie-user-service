package in.gov.ipie.service.user.consent;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One version of one consent notice - see {@code V34}'s header for why versions are never edited. */
@Entity
@Table(name = "consent_notices")
public class ConsentNoticeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "summary", nullable = false)
    private String summary;

    @Column(name = "document_uri")
    private String documentUri;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public int getVersion() {
        return version;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public String getSummary() {
        return summary;
    }

    public String getDocumentUri() {
        return documentUri;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
