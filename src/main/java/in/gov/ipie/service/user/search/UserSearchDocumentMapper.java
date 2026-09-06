package in.gov.ipie.service.user.search;

import java.util.UUID;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.user.domain.RegistrationStatus;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserStatus;

/**
 * Converts between the domain {@code User} and its {@link UserDocument} projection. Public so the
 * {@code searchindex} sibling subpackage and {@code UserSearchIndexConfig} can use it across the
 * package boundary.
 */
@Component
public class UserSearchDocumentMapper {

    public UserDocument toDocument(User user) {
        return new UserDocument(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getStatus().name(),
                user.getAuditMetadata());
    }

    public User toDomain(UserDocument document) {
        AuditMetadata auditMetadata = new AuditMetadata(
                document.getCreatedAt(), document.getCreatedBy(), document.getUpdatedAt(), document.getUpdatedBy(),
                document.getVersion(), document.isActive(), document.getDeletedAt(), document.getDeletedBy());
        return User.builder()
                .id(UUID.fromString(document.getId()))
                .username(document.getUsername())
                .email(document.getEmail())
                .fullName(document.getFullName())
                .phoneNumber(document.getPhoneNumber())
                .status(UserStatus.valueOf(document.getStatus()))
                // Not indexed in Elasticsearch (this read-model predates self-registration and only
                // backs the generic "search users" listing) - VERIFIED is the correct default for
                // every row it actually indexes today (createNew() always sets it), and this path
                // does not feed RegistrationController's own JPA-backed lookups.
                .registrationStatus(RegistrationStatus.VERIFIED)
                .auditMetadata(auditMetadata)
                .build();
    }
}

