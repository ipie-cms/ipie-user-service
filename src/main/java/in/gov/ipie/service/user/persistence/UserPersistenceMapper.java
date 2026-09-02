package in.gov.ipie.service.user.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.UUID;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.model.AuditMetadata;
import in.gov.ipie.service.user.domain.ProfessionalRoleHolding;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.common.utils.id.IdGenerator;

/**
 * Converts between the JPA entity and the domain model. A hand-written mapper (rather than
 * MapStruct) because it also assembles the {@link AuditMetadata} value object from five separate
 * entity columns - see {@code UserApiMapper} for the MapStruct-based mapping the master standards
 * doc calls for (5.2: "Use MapStruct for complex mappings") between API DTOs and the domain model.
 * Public so the {@code repository} subpackage - the only other caller - can use it across the
 * package boundary.
 */
@Component
public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        AuditMetadata auditMetadata = new AuditMetadata(
                entity.getCreatedAt(), entity.getCreatedBy(), entity.getUpdatedAt(), entity.getUpdatedBy(), entity.getVersion(),
                entity.isActive(), entity.getDeletedAt(), entity.getDeletedBy());
        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .phoneNumber(entity.getPhoneNumber())
                .status(entity.getStatus())
                .registrationStatus(entity.getRegistrationStatus())
                .keycloakUserId(entity.getKeycloakUserId())
                .verificationTokenHash(entity.getVerificationTokenHash())
                .verificationTokenExpiresAt(entity.getVerificationTokenExpiresAt())
                .verifiedAt(entity.getVerifiedAt())
                .organisationId(entity.getOrganisationId())
                .pillarScope(entity.getPillarScope())
                .notificationChannels(entity.getNotificationChannels())
                .category(entity.getCategory())
                .addressLine1(entity.getAddressLine1())
                .addressLine2(entity.getAddressLine2())
                .country(entity.getCountry())
                .state(entity.getState())
                .city(entity.getCity())
                .pin(entity.getPin())
                .identityProofTypeId(entity.getIdentityProofTypeId())
                .identityProofNumberHash(entity.getIdentityProofNumberHash())
                .identityProofNumberLast4(entity.getIdentityProofNumberLast4())
                .professionalRoles(toDomainRoles(entity.getProfessionalRoles()))
                .emailOtpCodeHash(entity.getEmailOtpCodeHash())
                .emailOtpExpiresAt(entity.getEmailOtpExpiresAt())
                .emailOtpAttempts(entity.getEmailOtpAttempts())
                .emailOtpResendCount(entity.getEmailOtpResendCount())
                .emailVerifiedAt(entity.getEmailVerifiedAt())
                .auditMetadata(auditMetadata)
                .build();
    }

    public UserJpaEntity toNewEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity(
                user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getPhoneNumber(),
                user.getStatus(), user.getRegistrationStatus());
        entity.setKeycloakUserId(user.getKeycloakUserId());
        entity.setVerificationTokenHash(user.getVerificationTokenHash());
        entity.setVerificationTokenExpiresAt(user.getVerificationTokenExpiresAt());
        entity.setVerifiedAt(user.getVerifiedAt());
        entity.setOrganisationId(user.getOrganisationId());
        entity.setPillarScope(user.getPillarScope());
        entity.setNotificationChannels(user.getNotificationChannels());
        copyRegistrationWizardFieldsOnto(user, entity);
        applyProfessionalRoles(entity, user);
        return entity;
    }

    public void copyMutableFieldsOnto(User user, UserJpaEntity entity) {
        entity.setEmail(user.getEmail());
        entity.setFullName(user.getFullName());
        entity.setPhoneNumber(user.getPhoneNumber());
        entity.setStatus(user.getStatus());
        entity.setRegistrationStatus(user.getRegistrationStatus());
        entity.setKeycloakUserId(user.getKeycloakUserId());
        entity.setVerificationTokenHash(user.getVerificationTokenHash());
        entity.setVerificationTokenExpiresAt(user.getVerificationTokenExpiresAt());
        entity.setVerifiedAt(user.getVerifiedAt());
        entity.setOrganisationId(user.getOrganisationId());
        entity.setPillarScope(user.getPillarScope());
        entity.setNotificationChannels(user.getNotificationChannels());
        // Copied through here (not just at draft-save/complete time) purely so an unrelated update
        // (e.g. PUT .../users/{id} changing phoneNumber) can't silently null out values only ever
        // set by the registration wizard or seed data.
        copyRegistrationWizardFieldsOnto(user, entity);
        // Completing a registration updates the pre-registered row rather than inserting a new
        // one, so the roles have to be applied on this path too - only doing it in toNewEntity meant
        // they were silently dropped for every real registration.
        applyProfessionalRoles(entity, user);
    }

    private void copyRegistrationWizardFieldsOnto(User user, UserJpaEntity entity) {
        entity.setCategory(user.getCategory());
        entity.setAddressLine1(user.getAddressLine1());
        entity.setAddressLine2(user.getAddressLine2());
        entity.setCountry(user.getCountry());
        entity.setState(user.getState());
        entity.setCity(user.getCity());
        entity.setPin(user.getPin());
        entity.setIdentityProofTypeId(user.getIdentityProofTypeId());
        entity.setIdentityProofNumberHash(user.getIdentityProofNumberHash());
        entity.setIdentityProofNumberLast4(user.getIdentityProofNumberLast4());
        entity.setEmailOtpCodeHash(user.getEmailOtpCodeHash());
        entity.setEmailOtpExpiresAt(user.getEmailOtpExpiresAt());
        // Both directions matter: this mapper is hand-written, so a field missing here is dropped
        // silently. Losing these two would reset the counters on every save and leave the OTP caps
        // permanently disarmed while still looking implemented.
        entity.setEmailOtpAttempts(user.getEmailOtpAttempts());
        entity.setEmailOtpResendCount(user.getEmailOtpResendCount());
        entity.setEmailVerifiedAt(user.getEmailVerifiedAt());
    }

    /**
     * Rows to holdings. Sorted by role id so a user's roles come back in a stable order - the
     * database returns no particular one, and an order that changes between reads makes a response
     * diff look like a change when nothing changed.
     */
    private List<ProfessionalRoleHolding> toDomainRoles(List<UserProfessionalRoleJpaEntity> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .sorted(Comparator.comparing(UserProfessionalRoleJpaEntity::getProfessionalRoleId))
                .map(row -> ProfessionalRoleHolding.builder()
                        .roleId(row.getProfessionalRoleId())
                        .identificationTypeId(row.getProfessionalIdentificationTypeId())
                        .identificationValue(row.getProfessionalIdentificationValue())
                        .legalRepresentativeTypeId(row.getLegalRepresentativeTypeId())
                        .build())
                .toList();
    }

    /**
     * Holdings onto the entity's own collection, reconciled by role.
     *
     * <p>Reconciled rather than cleared and refilled. Clearing and re-adding looks equivalent and is
     * not: within one flush Hibernate orders the inserts before the orphan deletes, so re-saving a
     * user with the roles they already had violates the unique constraint on (user_id,
     * professional_role_id). That surfaced as a 23505 on every account-provisioning event, which
     * then dead-lettered - a failure a long way from its cause.
     *
     * <p>Reconciling also keeps each row's identity and audit stamps, so "when did this person claim
     * this role" survives an unrelated update to their address.
     */
    private void applyProfessionalRoles(UserJpaEntity entity, User user) {
        List<ProfessionalRoleHolding> holdings =
                user.getProfessionalRoles() == null ? List.of() : user.getProfessionalRoles();

        Map<UUID, UserProfessionalRoleJpaEntity> existing = entity.getProfessionalRoles().stream()
                .collect(Collectors.toMap(UserProfessionalRoleJpaEntity::getProfessionalRoleId,
                        row -> row, (a, b) -> a, LinkedHashMap::new));

        Set<UUID> wanted = holdings.stream()
                .map(ProfessionalRoleHolding::roleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        entity.getProfessionalRoles().removeIf(row -> !wanted.contains(row.getProfessionalRoleId()));

        for (ProfessionalRoleHolding holding : holdings) {
            UserProfessionalRoleJpaEntity row = existing.get(holding.roleId());
            if (row == null) {
                row = new UserProfessionalRoleJpaEntity();
                row.setId(IdGenerator.newUuid());
                row.setProfessionalRoleId(holding.roleId());
                entity.getProfessionalRoles().add(row);
            }
            row.setProfessionalIdentificationTypeId(holding.identificationTypeId());
            row.setProfessionalIdentificationValue(holding.identificationValue());
            row.setLegalRepresentativeTypeId(holding.legalRepresentativeTypeId());
        }
    }
}
