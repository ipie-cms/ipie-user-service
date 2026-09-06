package in.gov.ipie.service.user.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

import in.gov.ipie.common.core.model.AuditMetadata;

/**
 * The User domain model - deliberately independent of the JPA entity in the infrastructure layer
 * (master standards doc, section 16: "Public APIs must not expose persistence entities", and more
 * generally the layers must stay independent so the persistence model can change without touching
 * business rules).
 *
 * <p>{@code @Getter} generates JavaBean-style getters (rather than record-style accessors) for
 * every field below, so {@code UserApiMapper}'s MapStruct interface can map this class
 * automatically by property name (see {@code UserApiMapper.toResponse}) - this class is also the
 * platform's one live combination of Lombok and MapStruct on the same type (master standards
 * doc, section 5's Lombok convention, "MapStruct interaction"), which is what that section's
 * {@code lombok-mapstruct-binding} guidance is tested against.
 *
 * <p>{@code @Builder} (rather than a hand-written all-args constructor) is what keeps this class
 * under Checkstyle's {@code ParameterNumber} limit (max 7) now that the registration-lifecycle
 * fields live here too - the generated constructor Lombok attaches the builder to is synthesized
 * at compile time, so it never appears as a long parameter list in this source file. Prefer the
 * {@link #createNew} / {@link #preRegister} factories over calling {@link #builder()} directly;
 * they encode the two valid starting states.
 */
@Getter
@Builder(toBuilder = true)
public final class User {

    private final UUID id;
    private final String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private UserStatus status;
    private final AuditMetadata auditMetadata;

    /** Independent of {@link #status} - see this class's Javadoc. */
    private RegistrationStatus registrationStatus;

    /** Set once {@link #accountProvisioned} records the Keycloak account; {@code null} until then. */
    private UUID keycloakUserId;

    /**
     * Opaque one-time token emailed to the <b>pillar admin</b>, authorising them to approve
     * this registration; cleared once {@link #verify} consumes it.
     *
     * <p>This service holds no credential-setting token and never has. The link that lets a
     * registrant choose their password is issued and validated by ipie-iam-service, which owns
     * credentials (ARCHITECTURE_WORKING_PLAN.md, §4.1.1) - a token that can take over an account has no
     * business in the service that owns the person's profile.
     */
    private String verificationTokenHash;

    private Instant verificationTokenExpiresAt;

    private Instant verifiedAt;

    /** {@code null} for an individual user (FRS 1.1.1) - see {@link Organisation}. */
    private UUID organisationId;

    /**
     * The pillar that validates this principal - IBBI for an insolvency professional or registered
     * valuer, whose standing rests on an IBBI registration number - or, when the user holds
     * PILLAR_ADMIN, the pillar they administer. Null for anyone no pillar validated: an entity admin
     * and the users beneath them, who are placed by {@code organisationId} instead.
     *
     * <p>The two are independent and either may be null. An IP sits in no hierarchy and carries a
     * pillar; an entity admin roots a hierarchy and carries none. See {@code VisibilityScope}.
     */
    private String pillarScope;

    /**
     * Which channel(s) this user has opted into for account notifications (registration status,
     * login alerts, ...) - never {@code null}/empty on a persisted user; {@link #defaultChannels()}
     * is applied by {@link #createNew}/{@link #preRegister} whenever the caller doesn't specify one.
     */
    private Set<NotificationChannel> notificationChannels;

    /** FRS 1.1.1 "Select Category" - {@code null} until the registration wizard's Personal Details step. */
    private AccountCategory category;

    private String addressLine1;
    private String addressLine2;
    private String country;
    private String state;
    private String city;
    private String pin;

    /** Paired with the two fields below - a FK id into the {@code identity_proof_types} lookup table. */
    private UUID identityProofTypeId;
    /**
     * Peppered HMAC of the identity proof number - never the number (Aadhaar Act s.29, Stage 5
     * item 4). Answers "is this the same number?" and nothing else.
     */
    private String identityProofNumberHash;

    /** The last four digits, kept so a person can tell which document this is. */
    private String identityProofNumberLast4;

    /**
     * Every professional role this person holds, each with the credential proving it - empty until
     * the registration wizard's Professional Details step.
     *
     * <p>A list, not a single value: the FRS allows several roles to be selected and IBBI confirmed
     * that one account covers every role an Insolvency Professional performs. See
     * {@link ProfessionalRoleHolding} for why the credential belongs to the holding rather than to
     * the person.
     */
    private List<ProfessionalRoleHolding> professionalRoles;

    /**
     * Peppered HMAC of the one-time numeric code emailed to the registering user's own address -
     * never the code itself (Stage 5, item 2). Cleared once {@link #confirmEmailOtp} succeeds.
     */
    private String emailOtpCodeHash;
    private Instant emailOtpExpiresAt;

    /** Failed guesses against the current code; reset whenever a new one is issued. */
    private int emailOtpAttempts;

    /** Codes ever issued for this registration. Never reset - it is what bounds the total guesses. */
    private int emailOtpResendCount;

    /** Set once {@link #confirmEmailOtp} accepts the right code before it expires; gates {@code completeRegistration}. */
    private Instant emailVerifiedAt;

    /** An account created directly (not through self-registration) - already verified, matching prior behaviour. */
    public static User createNew(String username, String email, String fullName, String phoneNumber) {
        return createNew(username, email, fullName, phoneNumber, null);
    }

    /** As {@link #createNew(String, String, String, String)}, with an explicit notification-channel preference. */
    public static User createNew(
            String username, String email, String fullName, String phoneNumber, Set<NotificationChannel> notificationChannels) {
        return User.builder()
                .username(username)
                .email(email)
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.VERIFIED)
                .notificationChannels(defaultIfEmpty(notificationChannels))
                .build();
    }

    /**
     * Step 1 of self-registration: only a mobile number and email are known yet. {@code username}
     * is set to {@code email} immediately (rather than left {@code null}) since it is both
     * {@code NOT NULL} and unique at the persistence layer, and email is already a stable,
     * known-unique identifier at this point.
     */
    public static User preRegister(String mobileNumber, String email) {
        return preRegister(mobileNumber, email, null);
    }

    /** As {@link #preRegister(String, String)}, with an explicit notification-channel preference. */
    public static User preRegister(String mobileNumber, String email, Set<NotificationChannel> notificationChannels) {
        return User.builder()
                .username(email)
                .email(email)
                .phoneNumber(mobileNumber)
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.PRE_REGISTRATION)
                .notificationChannels(defaultIfEmpty(notificationChannels))
                .build();
    }

    /** {@code EMAIL} only - the one channel with a real backing implementation today (see {@link NotificationChannel}). */
    private static Set<NotificationChannel> defaultChannels() {
        return Set.of(NotificationChannel.EMAIL);
    }

    private static Set<NotificationChannel> defaultIfEmpty(Set<NotificationChannel> notificationChannels) {
        return notificationChannels == null || notificationChannels.isEmpty() ? defaultChannels() : Set.copyOf(notificationChannels);
    }

    /**
     * Step 2: the registration form is complete and account creation has been requested. Stores the
     * the one-time approval token and moves to {@code PROVISIONING}.
     *
     * <p>No Keycloak id yet: creating the account is now asynchronous, so this method records the
     * request rather than its result. {@link #accountProvisioned} closes the loop when
     * ipie-iam-service reports the account exists.
     */
    public void completeRegistration(String fullName) {
        this.fullName = fullName;
        this.registrationStatus = RegistrationStatus.PROVISIONING;
    }

    /**
     * Step 3: ipie-iam-service has created the Keycloak account - records its id and moves to
     * {@code UNVERIFIED}, which is what releases the verification email.
     *
     * <p>Idempotent by necessity, not by preference: the event carrying this is delivered at least
     * once, so a redelivery must not move an already-verified user backwards. A user who has since
     * been verified keeps that status.
     */
    public void accountProvisioned(UUID keycloakUserId, String verificationTokenHash, Instant verificationTokenExpiresAt) {
        this.keycloakUserId = keycloakUserId;
        // The approval token is minted HERE rather than at completeRegistration, because this is the
        // step that releases the email carrying it. Two consequences, both wanted: the 48h window
        // starts when the pillar admin can actually act on it rather than while provisioning
        // was still in flight, and the caller only ever hands this class the digest - the plaintext
        // goes straight onto the event and is never stored (Stage 5, item 2).
        this.verificationTokenHash = verificationTokenHash;
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
        if (this.registrationStatus == RegistrationStatus.PROVISIONING) {
            this.registrationStatus = RegistrationStatus.UNVERIFIED;
        }
    }

    /**
     * A fresh email OTP was generated for this still-{@code PRE_REGISTRATION} row - see {@code
     * UserServiceImpl#requestEmailOtp}. Overwrites any earlier, unconfirmed code.
     */
    public void requestEmailOtp(String codeHash, Instant expiresAt) {
        this.emailOtpCodeHash = codeHash;
        this.emailOtpExpiresAt = expiresAt;
        // A new code gets a fresh allowance of guesses, but the resend count carries: otherwise
        // "ask for another code" resets the limit and the cap bounds nothing.
        this.emailOtpAttempts = 0;
        this.emailOtpResendCount++;
    }

    /**
     * Records a wrong guess and reports whether the allowance is now spent.
     *
     * @return {@code true} if this attempt exhausted {@code maxAttempts}, in which case the caller
     *     must discard the code - see {@link #invalidateEmailOtp}
     */
    public boolean recordFailedEmailOtpAttempt(int maxAttempts) {
        this.emailOtpAttempts++;
        return this.emailOtpAttempts >= maxAttempts;
    }

    /**
     * Discards the current code without granting anything.
     *
     * <p>Deliberately not a timed lockout on the account: an attacker who can reach this endpoint
     * could then lock any registration out at will, turning a guessing limit into a denial of
     * service against the legitimate registrant. Spending the code costs the attacker a resend and
     * costs the registrant one click.
     */
    public void invalidateEmailOtp() {
        this.emailOtpCodeHash = null;
        this.emailOtpExpiresAt = null;
    }

    /** Whether another code may be issued at all - see {@link #emailOtpResendCount}. */
    public boolean canRequestAnotherEmailOtp(int maxResends) {
        return this.emailOtpResendCount < maxResends;
    }

    /** The submitted code matched and hadn't expired - validity was already checked by the caller (mirrors {@link #verify}). */
    public void confirmEmailOtp() {
        this.emailVerifiedAt = Instant.now();
        this.emailOtpCodeHash = null;
        this.emailOtpExpiresAt = null;
        this.emailOtpAttempts = 0;
    }

    /**
     * The pillar admin followed the emailed link - token validity was already checked by the
     * caller.
     *
     * <p>Approval and password-setting are independent steps that can happen in either order. The
     * registrant's setup token lives in ipie-iam-service and is untouched by this, so approving a
     * user who has not yet chosen a password does not strand them.
     */
    public void verify() {
        this.registrationStatus = RegistrationStatus.VERIFIED;
        this.verifiedAt = Instant.now();
        this.verificationTokenHash = null;
        this.verificationTokenExpiresAt = null;
    }

    public void updateDetails(String newEmail, String newFullName, String newPhoneNumber) {
        this.email = newEmail;
        this.fullName = newFullName;
        this.phoneNumber = newPhoneNumber;
    }

    /** {@code organisationId} may be {@code null} to un-affiliate (FRS 1.1.1: individual users have none). */
    public void affiliateWithOrganisation(UUID organisationId) {
        this.organisationId = organisationId;
    }

    /** {@code channels} must be non-null/non-empty - callers choose at least one opted channel. */
    public void updateNotificationChannels(Set<NotificationChannel> channels) {
        this.notificationChannels = Set.copyOf(channels);
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void reactivate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
