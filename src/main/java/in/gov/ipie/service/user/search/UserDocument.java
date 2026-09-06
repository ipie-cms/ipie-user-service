package in.gov.ipie.service.user.search;

import java.time.Instant;
import java.util.Locale;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import in.gov.ipie.common.core.model.AuditMetadata;

/**
 * Elasticsearch representation of a user, indexed purely for the "search users" read path. Never
 * returned from an API and never referenced outside the {@code infrastructure.search} package tree
 * (master standards doc, section 16) - {@code UserSearchDocumentMapper} converts to/from the
 * domain {@code User} at the port boundary, the same way {@code UserPersistenceMapper} does for
 * the JPA entity.
 *
 * <p>{@code username}/{@code email} are indexed twice: once as {@code keyword} for exact
 * sort/display, and once lower-cased ({@code usernameLower}/{@code emailLower}) for the
 * case-insensitive "contains" search {@code ElasticsearchUserSearchIndex} runs as a wildcard
 * query - matching {@code UserSpecifications}' {@code cb.lower(...) LIKE %pattern%} semantics on
 * the JPA side, without relying on analyzer behaviour for correctness.
 */
@Document(indexName = "users")
public class UserDocument {

    @Id
    private String id;

    // Duplicates `id` as a regular, sortable field - `@Id` alone only becomes Elasticsearch's
    // `_id` metafield, which is not reliably usable as a sort/search_after tiebreaker. Same
    // dual-field pattern as username/usernameLower below, applied for sorting instead of search.
    @Field(type = FieldType.Keyword)
    private String idSort;

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Keyword)
    private String usernameLower;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Keyword)
    private String emailLower;

    @Field(type = FieldType.Text)
    private String fullName;

    @Field(type = FieldType.Keyword)
    private String phoneNumber;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Keyword)
    private String createdBy;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    @Field(type = FieldType.Keyword)
    private String updatedBy;

    @Field(type = FieldType.Long)
    private long version;

    @Field(type = FieldType.Boolean)
    private boolean isActive;

    @Field(type = FieldType.Date)
    private Instant deletedAt;

    @Field(type = FieldType.Keyword)
    private String deletedBy;

    protected UserDocument() {
        // required by Spring Data Elasticsearch
    }

    public UserDocument(String id, String username, String email, String fullName, String phoneNumber,
                         String status, AuditMetadata auditMetadata) {
        this.id = id;
        this.idSort = id;
        this.username = username;
        this.usernameLower = username.toLowerCase(Locale.ROOT);
        this.email = email;
        this.emailLower = email.toLowerCase(Locale.ROOT);
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.createdAt = auditMetadata != null ? auditMetadata.createdAt() : null;
        this.createdBy = auditMetadata != null ? auditMetadata.createdBy() : null;
        this.updatedAt = auditMetadata != null ? auditMetadata.updatedAt() : null;
        this.updatedBy = auditMetadata != null ? auditMetadata.updatedBy() : null;
        this.version = auditMetadata != null ? auditMetadata.version() : 0;
        this.isActive = auditMetadata == null || auditMetadata.isActive();
        this.deletedAt = auditMetadata != null ? auditMetadata.deletedAt() : null;
        this.deletedBy = auditMetadata != null ? auditMetadata.deletedBy() : null;
    }

    public String getId() {
        return id;
    }

    public String getIdSort() {
        return idSort;
    }

    public String getUsername() {
        return username;
    }

    public String getUsernameLower() {
        return usernameLower;
    }

    public String getEmail() {
        return email;
    }

    public String getEmailLower() {
        return emailLower;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public long getVersion() {
        return version;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }
}

