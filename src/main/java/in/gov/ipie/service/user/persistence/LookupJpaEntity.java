package in.gov.ipie.service.user.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.UuidGenerator;

import in.gov.ipie.common.utils.id.UuidV7Generator;

/**
 * Shared shape for every "closed list that can grow without a code change" lookup table
 * (professional roles, legal representative types, professional identification types, identity
 * proof types, ...) - a registration-wizard dropdown reads {@code code}/{@code label} straight
 * from the database (see {@code RegistrationLookupsController}) instead of a hardcoded enum, so
 * adding a new option is a seed-data insert, not a deploy. Deliberately no audit/soft-delete
 * columns (unlike {@code AuditableJpaEntity}) - these rows are only ever managed via migrations,
 * never through a write API, so there's nothing to audit. Deliberately no Lombok, matching this
 * package's own convention (see {@code UserJpaEntity}'s Javadoc) - only {@code
 * AuditableJpaEntity} in common-libs uses it.
 */
@MappedSuperclass
public abstract class LookupJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    /**
     * Stable, machine-readable key (e.g. {@code "INSOLVENCY_PROFESSIONAL"}) - what {@code User}'s
     * FK columns reference by id, not by this.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** Human-readable text shown in the dropdown. */
    @Column(nullable = false, length = 150)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** A row can be retired (excluded from new registrations) without deleting it - existing users may still reference it. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }
}
