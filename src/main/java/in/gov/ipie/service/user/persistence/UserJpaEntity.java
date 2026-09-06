package in.gov.ipie.service.user.persistence;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import in.gov.ipie.common.utils.id.UuidV7Generator;

import in.gov.ipie.common.persistence.AuditableJpaEntity;
import in.gov.ipie.service.user.domain.AccountCategory;
import in.gov.ipie.service.user.domain.NotificationChannel;
import in.gov.ipie.service.user.domain.RegistrationStatus;
import in.gov.ipie.service.user.domain.UserStatus;

/**
 * JPA representation of a user. Never returned from an API and never referenced outside the
 * infrastructure layer (master standards doc, section 16) - {@code UserPersistenceMapper}
 * converts to/from the domain {@code User} at the repository boundary. Standard audit + soft-delete
 * columns are inherited from {@link AuditableJpaEntity} (master standards doc, 7.2).
 */
@Entity
@Table(name = "users")
public class UserJpaEntity extends AuditableJpaEntity {

    @Id
    @UuidGenerator(algorithm = UuidV7Generator.class)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    /**
     * Whether this principal is an entity rather than a person - and so whether its detail is in
     * {@code organisations} or in {@code person} (V13).
     *
     * <p>A cached copy of a fact that became derivable the moment the detail moved out, kept because
     * "is this principal an entity" is asked on every authorisation path and should not cost a join.
     * Being denormalised, it can disagree with reality, so it is written in the same transaction as
     * the detail row it describes and never set on its own: {@link #forEntity} is the only way to
     * raise it, and it creates a principal that has no person detail by construction.
     */
    @Column(name = "is_org", nullable = false)
    private boolean isOrg;

    /**
     * The person detail, for a principal that is a person. Cascaded and orphan-removed because the
     * row means nothing without this one, and fetched eagerly because every read of a user reads a
     * name - a lazy association here would be an N+1 on the busiest query in the service.
     *
     * <p>Null exactly when {@link #isOrg} is true. The accessors below read through it, so the shape
     * of the row changed without the mapper, the services or the API noticing.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private PersonJpaEntity person;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 20)
    private RegistrationStatus registrationStatus;

    @Column(name = "keycloak_user_id")
    private UUID keycloakUserId;

    @Column(name = "verification_token_hash", length = 64)
    private String verificationTokenHash;

    @Column(name = "verification_token_expires_at")
    private Instant verificationTokenExpiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    // Nullable: a normal individual user has no organisation (FRS 1.1.1's "Account Type:
    // Individual" vs "Entity") - see OrganisationJpaEntity.
    @Column(name = "organisation_id")
    private UUID organisationId;

    /**
     * The pillar that validates this principal, or - when the user holds PILLAR_ADMIN - the pillar
     * they administer. Null for anyone no pillar validated. See V9 and {@code VisibilityScope}.
     */
    @Column(name = "pillar_scope", length = 10)
    private String pillarScope;

    // Set via the setter below, not the constructor - the constructor is already at Checkstyle's
    // ParameterNumber limit (7); see UserPersistenceMapper.toNewEntity, which sets this the same
    // way it already sets keycloakUserId/verificationTokenHash/etc.
    @Convert(converter = NotificationChannelsConverter.class)
    @Column(name = "notification_channels", nullable = false, length = 50)
    private Set<NotificationChannel> notificationChannels;

    /**
     * Every professional role held, one row each (V5). Owned by the user - the rows mean nothing
     * without them - so they are written and removed with the user rather than managed separately.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private List<UserProfessionalRoleJpaEntity> professionalRoles = new ArrayList<>();

    @Column(name = "email_otp_code_hash", length = 64)
    private String emailOtpCodeHash;

    @Column(name = "email_otp_attempts", nullable = false)
    private int emailOtpAttempts;

    @Column(name = "email_otp_resend_count", nullable = false)
    private int emailOtpResendCount;

    @Column(name = "email_otp_expires_at")
    private Instant emailOtpExpiresAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    protected UserJpaEntity() {
        // required by JPA
    }

    /**
     * A person principal. The detail row is created here rather than on first write, because a
     * principal with {@code isOrg} false and no person row is invisible to every screen that reads a
     * name - which reads as missing data rather than as a broken invariant. A PRE_REGISTRATION draft
     * has nothing but a username and an email, and still gets its row.
     */
    public UserJpaEntity(
            UUID id, String username, String email, String fullName, String phoneNumber, UserStatus status,
            RegistrationStatus registrationStatus) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.status = status;
        this.registrationStatus = registrationStatus;
        this.person = new PersonJpaEntity(this);
        this.person.setFullName(fullName);
        this.person.setPhoneNumber(phoneNumber);
    }

    /**
     * An entity principal - the users row an organisation is, with no person detail by construction.
     *
     * <p>Its username and email are derived from the government id the organisation already carries,
     * because both are UNIQUE NOT NULL here and an organisation has neither of its own. The address
     * uses the reserved {@code .invalid} domain deliberately: it can never be delivered to, so
     * nothing can quietly start mailing an entity principal in place of the person authorised to act
     * for it.
     */
    public static UserJpaEntity forEntity(String idType, String idValue) {
        String handle = (idType + "-" + idValue).toLowerCase(Locale.ROOT);
        UserJpaEntity entity = new UserJpaEntity();
        entity.username = handle;
        entity.email = handle + "@entity.invalid";
        entity.status = UserStatus.ACTIVE;
        // Not PRE_REGISTRATION: no registration workflow was ever started for an entity principal
        // and none will be - it is the authorised representative who registers, and their own row
        // carries that lifecycle.
        entity.registrationStatus = RegistrationStatus.VERIFIED;
        entity.isOrg = true;
        entity.notificationChannels = Set.of(NotificationChannel.EMAIL);
        return entity;
    }

    /**
     * The person detail, demanded rather than requested. An entity principal has none and never
     * will, so asking for it is a bug in the caller and not a null to absorb.
     */
    private PersonJpaEntity person() {
        if (isOrg) {
            throw new IllegalStateException(
                    "User " + id + " is an entity principal and has no person detail; its detail is in organisations");
        }
        if (person == null) {
            person = new PersonJpaEntity(this);
        }
        return person;
    }

    public boolean isOrg() {
        return isOrg;
    }

    public PersonJpaEntity getPerson() {
        return person;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return person == null ? null : person.getFullName();
    }

    public void setFullName(String fullName) {
        person().setFullName(fullName);
    }

    public String getPhoneNumber() {
        return person == null ? null : person.getPhoneNumber();
    }

    public void setPhoneNumber(String phoneNumber) {
        person().setPhoneNumber(phoneNumber);
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public UUID getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(UUID keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    public String getVerificationTokenHash() {
        return verificationTokenHash;
    }

    public void setVerificationTokenHash(String verificationTokenHash) {
        this.verificationTokenHash = verificationTokenHash;
    }

    public Instant getVerificationTokenExpiresAt() {
        return verificationTokenExpiresAt;
    }

    public void setVerificationTokenExpiresAt(Instant verificationTokenExpiresAt) {
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public void setOrganisationId(UUID organisationId) {
        this.organisationId = organisationId;
    }

    public String getPillarScope() {
        return pillarScope;
    }

    public void setPillarScope(String pillarScope) {
        this.pillarScope = pillarScope;
    }

    public Set<NotificationChannel> getNotificationChannels() {
        return notificationChannels;
    }

    public void setNotificationChannels(Set<NotificationChannel> notificationChannels) {
        this.notificationChannels = notificationChannels;
    }



    public AccountCategory getCategory() {
        return person == null ? null : person.getCategory();
    }

    public void setCategory(AccountCategory category) {
        person().setCategory(category);
    }

    public String getAddressLine1() {
        return person == null ? null : person.getAddressLine1();
    }

    public void setAddressLine1(String addressLine1) {
        person().setAddressLine1(addressLine1);
    }

    public String getAddressLine2() {
        return person == null ? null : person.getAddressLine2();
    }

    public void setAddressLine2(String addressLine2) {
        person().setAddressLine2(addressLine2);
    }

    public String getCountry() {
        return person == null ? null : person.getCountry();
    }

    public void setCountry(String country) {
        person().setCountry(country);
    }

    public String getState() {
        return person == null ? null : person.getState();
    }

    public void setState(String state) {
        person().setState(state);
    }

    public String getCity() {
        return person == null ? null : person.getCity();
    }

    public void setCity(String city) {
        person().setCity(city);
    }

    public String getPin() {
        return person == null ? null : person.getPin();
    }

    public void setPin(String pin) {
        person().setPin(pin);
    }

    public UUID getIdentityProofTypeId() {
        return person == null ? null : person.getIdentityProofTypeId();
    }

    public void setIdentityProofTypeId(UUID identityProofTypeId) {
        person().setIdentityProofTypeId(identityProofTypeId);
    }

    public String getIdentityProofNumberHash() {
        return person == null ? null : person.getIdentityProofNumberHash();
    }

    public void setIdentityProofNumberHash(String identityProofNumberHash) {
        person().setIdentityProofNumberHash(identityProofNumberHash);
    }

    public String getIdentityProofNumberLast4() {
        return person == null ? null : person.getIdentityProofNumberLast4();
    }

    public void setIdentityProofNumberLast4(String identityProofNumberLast4) {
        person().setIdentityProofNumberLast4(identityProofNumberLast4);
    }







    public int getEmailOtpAttempts() {
        return emailOtpAttempts;
    }

    public void setEmailOtpAttempts(int emailOtpAttempts) {
        this.emailOtpAttempts = emailOtpAttempts;
    }

    public int getEmailOtpResendCount() {
        return emailOtpResendCount;
    }

    public void setEmailOtpResendCount(int emailOtpResendCount) {
        this.emailOtpResendCount = emailOtpResendCount;
    }

    public String getEmailOtpCodeHash() {
        return emailOtpCodeHash;
    }

    public void setEmailOtpCodeHash(String emailOtpCodeHash) {
        this.emailOtpCodeHash = emailOtpCodeHash;
    }

    public Instant getEmailOtpExpiresAt() {
        return emailOtpExpiresAt;
    }

    public void setEmailOtpExpiresAt(Instant emailOtpExpiresAt) {
        this.emailOtpExpiresAt = emailOtpExpiresAt;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public List<UserProfessionalRoleJpaEntity> getProfessionalRoles() {
        return professionalRoles;
    }

    public void setProfessionalRoles(List<UserProfessionalRoleJpaEntity> professionalRoles) {
        this.professionalRoles = professionalRoles;
    }
}
