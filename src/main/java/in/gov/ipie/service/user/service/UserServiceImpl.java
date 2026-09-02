package in.gov.ipie.service.user.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.common.audit.annotation.Auditable;
import in.gov.ipie.common.audit.model.AuditEventType;
import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.command.CompleteRegistrationCommand;
import in.gov.ipie.service.user.command.ConfirmEmailOtpCommand;
import in.gov.ipie.service.user.command.CreateOrganisationCommand;
import in.gov.ipie.service.user.command.CreateRegistrationCommand;
import in.gov.ipie.service.user.command.CreateUserCommand;
import in.gov.ipie.service.user.command.EntityDraftDetails;
import in.gov.ipie.service.user.command.SaveRegistrationDraftCommand;
import in.gov.ipie.service.user.command.UpdateUserCommand;
import in.gov.ipie.service.user.event.RegistrationEmailOtpRequestedPayload;
import in.gov.ipie.service.user.event.UserLoggedInPayload;
import in.gov.ipie.service.user.event.UserRegistrationCompletedPayload;
import in.gov.ipie.service.user.event.UserVerifiedPayload;
import in.gov.ipie.service.user.exception.EmailNotVerifiedException;
import in.gov.ipie.service.user.exception.EmailOtpAttemptsExhaustedException;
import in.gov.ipie.service.user.exception.EmailOtpResendLimitReachedException;
import in.gov.ipie.service.user.exception.InvalidOtpException;
import in.gov.ipie.service.user.exception.InvalidVerificationTokenException;
import in.gov.ipie.service.user.exception.RegistrationAlreadyCompletedException;
import in.gov.ipie.service.user.exception.UserNotFoundException;
import in.gov.ipie.service.user.consent.ConsentRecorder;
import in.gov.ipie.service.user.domain.NotificationChannel;
import in.gov.ipie.service.user.domain.RegistrationStatus;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.VisibilityScope;
import in.gov.ipie.service.user.repository.UserRepository;
import in.gov.ipie.service.user.security.RegistrationSecretHasher;
import in.gov.ipie.service.user.repository.UserSearchIndex;
import in.gov.ipie.service.user.event.AccountProvisioningRequestedPayload;
import in.gov.ipie.service.user.event.UserEventType;

/**
 * {@link UserService} implementation. Business rules (state transitions) live here and in the
 * domain model - controllers only translate HTTP <-> commands (master standards doc, 5.1/5.2:
 * "Keep controllers thin"). Username/email/mobile uniqueness validation lives in
 * {@link UserValidationAspect}, not here - see its Javadoc.
 *
 * <p>Events go through {@link UserEventPublisher}, which writes them to the outbox rather than to a
 * broker - the outbox row lands inside the same {@code @Transactional} boundary as the entity save,
 * which is what makes the two atomic (master standards doc, section 9).
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserSearchIndex userSearchIndex;
    private final UserEventPublisher eventPublisher;
    private final OrganisationService organisationService;
    /** Shared and thread-safe; seeded by the OS, and never reseeded from anything predictable. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** TTLs, attempt limits and the set-level rules on professional roles. */
    private final RegistrationPolicy registrationPolicy;

    /** Stage 5, item 2: what reaches the database is a peppered digest, never the secret itself. */
    private final RegistrationSecretHasher secretHasher;

    /** Stage 5, item 5: the ledger that proves what was agreed, beside the column that acts on it. */
    private final ConsentRecorder consentRecorder;

    public UserServiceImpl(
            UserRepository userRepository,
            UserSearchIndex userSearchIndex,
            UserEventPublisher eventPublisher,
            OrganisationService organisationService,
            RegistrationSecretHasher secretHasher,
            ConsentRecorder consentRecorder,
            RegistrationPolicy registrationPolicy) {
        this.userRepository = userRepository;
        this.userSearchIndex = userSearchIndex;
        this.eventPublisher = eventPublisher;
        this.organisationService = organisationService;
        this.secretHasher = secretHasher;
        this.consentRecorder = consentRecorder;
        this.registrationPolicy = registrationPolicy;
    }

    @Override
    @Transactional
    @Auditable(
            action = "USER_CREATED", entityType = "USER", entityId = "#result.id", eventType = AuditEventType.BUSINESS,
            newValue = "#result")
    public User createUser(CreateUserCommand command) {
        User user = User.createNew(
                command.username(), command.email(), command.fullName(), command.phoneNumber(), command.notificationChannels());
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        eventPublisher.publish(UserEventType.USER_CREATED, saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> searchUsers(UserSearchCriteria criteria, VisibilityScope visibleTo, PageRequest pageRequest) {
        return userSearchIndex.search(criteria, visibleTo, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResult<User> searchUsersAfter(UserSearchCriteria criteria, VisibilityScope visibleTo, CursorPageRequest pageRequest) {
        return userSearchIndex.searchAfter(criteria, visibleTo, pageRequest);
    }

    @Override
    @Transactional
    @Auditable(
            action = "USER_UPDATED", entityType = "USER", entityId = "#command.userId()", comment = "#command.comment()",
            newValue = "#result")
    public User updateUser(UpdateUserCommand command) {
        User user = userRepository.findById(command.userId()).orElseThrow(() -> new UserNotFoundException(command.userId()));
        user.updateDetails(command.email(), command.fullName(), command.phoneNumber());
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        eventPublisher.publish(UserEventType.USER_UPDATED, saved);
        return saved;
    }

    @Override
    @Transactional
    @Auditable(
            action = "USER_DEACTIVATED", entityType = "USER", entityId = "#userId", comment = "#comment",
            newValue = "#result")
    public User deactivateUser(UUID userId, String comment) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.deactivate();
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        eventPublisher.publish(UserEventType.USER_DEACTIVATED, saved);
        return saved;
    }

    @Override
    @Transactional
    @Auditable(
            action = "USER_REACTIVATED", entityType = "USER", entityId = "#userId", comment = "#comment",
            newValue = "#result")
    public User reactivateUser(UUID userId, String comment) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.reactivate();
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        eventPublisher.publish(UserEventType.USER_REACTIVATED, saved);
        return saved;
    }

    @Override
    @Transactional
    @Auditable(
            action = "REGISTRATION_CREATED", entityType = "USER", entityId = "#result.id", eventType = AuditEventType.BUSINESS,
            newValue = "#result")
    public User createRegistration(CreateRegistrationCommand command) {
        User user = User.preRegister(command.mobileNumber(), command.email(), command.notificationChannels());
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        // Recorded against the SAVED channels, not the requested ones: preRegister substitutes a
        // default when none are given, and the ledger must record what the person is actually
        // signed up to rather than what they typed.
        consentRecorder.recordInitialConsent(saved.getId(), saved.getNotificationChannels());
        return saved;
    }

    /**
     * Step 2 ("SUBMIT FOR VERIFICATION"): applies the full registration wizard payload, resolves
     * (finds-or-creates) the Entity organisation if one was captured, provisions the Keycloak
     * account (the user can log in from this point on), and moves to {@code UNVERIFIED} -
     * publishes {@link UserEventType#USER_REGISTRATION_COMPLETED} so ipie-communication-service
     * can email the pillar admin. Requires the email OTP to already be confirmed (see
     * {@link EmailNotVerifiedException}) - the wizard's real teeth for the "SEND OTP" step.
     */
    @Override
    @Transactional
    @Auditable(
            action = "REGISTRATION_COMPLETED", entityType = "USER", entityId = "#command.registrationId()",
            eventType = AuditEventType.BUSINESS, newValue = "#result")
    public User completeRegistration(CompleteRegistrationCommand command) {
        User user = userRepository.findById(command.registrationId())
                .orElseThrow(() -> new UserNotFoundException(command.registrationId()));
        if (user.getRegistrationStatus() != RegistrationStatus.PRE_REGISTRATION) {
            throw new RegistrationAlreadyCompletedException(command.registrationId());
        }
        if (user.getEmailVerifiedAt() == null) {
            throw new EmailNotVerifiedException(command.registrationId());
        }

        registrationPolicy.validateProfessionalRoles(command.professionalRoles());

        UUID organisationId = resolveOrganisationId(command.organisationId(), command.entity());
        User withRichFields = user.toBuilder()
                .category(command.category())
                .addressLine1(command.addressLine1())
                .addressLine2(command.addressLine2())
                .country(command.country())
                .state(command.state())
                .city(command.city())
                .pin(command.pin())
                .identityProofTypeId(command.identityProofTypeId())
                .identityProofNumberHash(secretHasher.hash(command.identityProofNumber()))
                .identityProofNumberLast4(lastFourDigits(command.identityProofNumber()))
                .professionalRoles(command.professionalRoles())
                .organisationId(organisationId)
                .build();

        String[] nameParts = splitFullName(command.fullName());
        // The pillar admin's approval token, and only that. The registrant's set-password link
        // is a different token for a different person, issued and validated by ipie-iam-service -
        // this service never holds one (ARCHITECTURE_WORKING_PLAN.md, §4.1.1).
        // No approval token yet: it is minted in accountProvisioned, the step that actually
        // releases the email carrying it.
        withRichFields.completeRegistration(command.fullName());

        User saved = userRepository.save(withRichFields);
        userSearchIndex.index(saved);

        // Ask ipie-iam-service to create the Keycloak account instead of calling it and waiting.
        // Registration used to succeed only if Keycloak was healthy at that instant, on a path a
        // citizen is sitting in front of; it now completes on this service's own database write,
        // and the account follows. The event deliberately carries no password - see the payload.
        //
        // USER_REGISTRATION_COMPLETED, which is what makes ipie-communication-service send the
        // verification email, is NOT published here any more. It follows in accountProvisioned()
        // once the account actually exists, so the link in that email always leads somewhere the
        // user can set a password.
        eventPublisher.publish(UserEventType.ACCOUNT_PROVISIONING_REQUESTED, new AccountProvisioningRequestedPayload(
                saved.getId(), saved.getUsername(), saved.getEmail(), nameParts[0], nameParts[1]));
        return saved;
    }

    /**
     * {@code explicitOrganisationId} (the wizard picked an already-registered Entity from search)
     * takes precedence over {@code entity} (a brand-new Entity's captured details); {@code null}
     * when neither is present (an Individual registration, or an Entity step not yet reached).
     */
    private UUID resolveOrganisationId(UUID explicitOrganisationId, EntityDraftDetails entity) {
        if (explicitOrganisationId != null) {
            return explicitOrganisationId;
        }
        if (entity == null || entity.idType() == null || entity.idValue() == null) {
            return null;
        }
        return organisationService.findOrCreate(new CreateOrganisationCommand(
                entity.name(), entity.legalConstitution(), entity.idType(), entity.idValue(), entity.msme(),
                entity.msmeType(), entity.registeredAddress(), entity.contactNumber(), entity.contactEmail(),
                entity.country(), entity.state(), entity.city(), entity.pin(), entity.district())).getId();
    }

    @Override
    @Transactional
    @Auditable(
            action = "REGISTRATION_DRAFT_SAVED", entityType = "USER", entityId = "#command.registrationId()",
            eventType = AuditEventType.BUSINESS, newValue = "#result")
    public User saveRegistrationDraft(SaveRegistrationDraftCommand command) {
        User user = userRepository.findById(command.registrationId())
                .orElseThrow(() -> new UserNotFoundException(command.registrationId()));
        if (user.getRegistrationStatus() != RegistrationStatus.PRE_REGISTRATION) {
            throw new RegistrationAlreadyCompletedException(command.registrationId());
        }

        registrationPolicy.validateProfessionalRoles(command.professionalRoles());

        UUID organisationId = resolveOrganisationId(command.organisationId(), command.entity());
        User withDraftFields = user.toBuilder()
                .fullName(command.fullName())
                .category(command.category())
                .addressLine1(command.addressLine1())
                .addressLine2(command.addressLine2())
                .country(command.country())
                .state(command.state())
                .city(command.city())
                .pin(command.pin())
                .identityProofTypeId(command.identityProofTypeId())
                .identityProofNumberHash(secretHasher.hash(command.identityProofNumber()))
                .identityProofNumberLast4(lastFourDigits(command.identityProofNumber()))
                .professionalRoles(command.professionalRoles())
                .organisationId(organisationId)
                .build();

        User saved = userRepository.save(withDraftFields);
        userSearchIndex.index(saved);
        return saved;
    }

    @Override
    @Transactional
    public User requestEmailOtp(UUID registrationId) {
        User user = userRepository.findById(registrationId).orElseThrow(() -> new UserNotFoundException(registrationId));
        if (user.getRegistrationStatus() != RegistrationStatus.PRE_REGISTRATION) {
            throw new RegistrationAlreadyCompletedException(registrationId);
        }
        // The cap that actually bounds a guessing attack: without it, "request another code" resets
        // the per-code attempt allowance and the search continues five guesses at a time.
        if (!user.canRequestAnotherEmailOtp(registrationPolicy.emailOtpMaxResends())) {
            throw new EmailOtpResendLimitReachedException();
        }

        // SecureRandom, not ThreadLocalRandom: this code is a security token, and
        // ThreadLocalRandom is a predictable PRNG whose output can be reconstructed from a few
        // observed values (CWE-338). The stored form is a hash - see RegistrationSecretHasher.
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        user.requestEmailOtp(secretHasher.hash(code), Instant.now().plus(registrationPolicy.emailOtpTtl()));
        User saved = userRepository.save(user);
        eventPublisher.publish(UserEventType.REGISTRATION_EMAIL_OTP_REQUESTED,
                new RegistrationEmailOtpRequestedPayload(saved.getId(), saved.getEmail(), code));
        return saved;
    }

    @Override
    // noRollbackFor is load-bearing, not a style choice: the failure paths below RECORD the attempt
    // and then throw. Under the default rollback-on-RuntimeException, that write would be undone by
    // the very exception that reports it, the counter would never move, and the cap would silently
    // never engage - a limit that looks implemented and is not.
    @Transactional(noRollbackFor = {InvalidOtpException.class, EmailOtpAttemptsExhaustedException.class})
    @Auditable(
            action = "REGISTRATION_EMAIL_VERIFIED", entityType = "USER", entityId = "#command.registrationId()",
            eventType = AuditEventType.BUSINESS, newValue = "#result")
    public User confirmEmailOtp(ConfirmEmailOtpCommand command) {
        User user = userRepository.findById(command.registrationId())
                .orElseThrow(() -> new UserNotFoundException(command.registrationId()));
        // Constant-time, via the hasher: comparing with equals() leaks through timing how many
        // leading characters matched, which is enough to recover a six-digit code a digit at a time.
        if (!secretHasher.matches(command.code(), user.getEmailOtpCodeHash())
                || user.getEmailOtpExpiresAt() == null || user.getEmailOtpExpiresAt().isBefore(Instant.now())) {
            boolean exhausted = user.recordFailedEmailOtpAttempt(registrationPolicy.emailOtpMaxAttempts());
            if (exhausted) {
                user.invalidateEmailOtp();
            }
            userRepository.save(user);
            throw exhausted ? new EmailOtpAttemptsExhaustedException() : new InvalidOtpException();
        }

        user.confirmEmailOtp();
        User saved = userRepository.save(user);
        return saved;
    }

    @Override
    @Transactional
    @Auditable(action = "ACCOUNT_PROVISIONED", entityType = "USER", entityId = "#userId", newValue = "#result")
    public User accountProvisioned(UUID userId, UUID keycloakUserId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        // Minted here, stored only as a hash, and carried in clear text no further than the event
        // that mails it - which §4.6 permits for a single-use, short-TTL secret. Once this method
        // returns, the plaintext exists nowhere in this service.
        // Random by requirement, NOT IdGenerator.newUuid(). This token is emailed and grants
        // approval of a registration, so it must be unguessable; a v7 id leaks its own creation
        // time and narrows the search from a neighbouring token. See IdGenerator's Javadoc.
        String verificationToken = UUID.randomUUID().toString();
        user.accountProvisioned(
                keycloakUserId, secretHasher.hash(verificationToken), Instant.now().plus(registrationPolicy.verificationTokenTtl()));
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);

        // Held back until now on purpose: this one event triggers both emails - the pillar
        // admin's approval link and the registrant's set-password link - and neither can be sent
        // before the account exists. Publishing it at completeRegistration time would send the user
        // to a page that could not work yet.
        eventPublisher.publish(UserEventType.USER_REGISTRATION_COMPLETED, new UserRegistrationCompletedPayload(
                saved.getId(), saved.getEmail(), saved.getFullName(), saved.getPhoneNumber(),
                verificationToken, saved.getNotificationChannels()));
        return saved;
    }

    @Override
    @Transactional
    @Auditable(
            action = "USER_VERIFIED", entityType = "USER", entityId = "#result.id", eventType = AuditEventType.BUSINESS,
            newValue = "#result")
    public User verifyByToken(String token) {
        // The caller presents the token from the emailed link; the column holds its digest, so the
        // lookup hashes first. Same value, same pepper, same row - and a database dump yields
        // nothing anyone can put in a URL.
        User user = userRepository.findByVerificationTokenHash(secretHasher.hash(token))
                .orElseThrow(InvalidVerificationTokenException::new);
        if (user.getVerificationTokenExpiresAt() == null || user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new InvalidVerificationTokenException();
        }

        user.verify();
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        eventPublisher.publish(UserEventType.USER_VERIFIED,
                new UserVerifiedPayload(saved.getId(), saved.getKeycloakUserId(), saved.getEmail()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(UUID keycloakUserId) {
        return userRepository.findByKeycloakUserId(keycloakUserId).orElseThrow(() -> new UserNotFoundException(keycloakUserId));
    }

    @Override
    @Transactional
    @Auditable(
            action = "USER_AFFILIATED_WITH_ORGANISATION", entityType = "USER", entityId = "#userId", comment = "#comment",
            newValue = "#result")
    public User affiliateWithOrganisation(UUID userId, UUID organisationId, String comment) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        user.affiliateWithOrganisation(organisationId);
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        return saved;
    }

    @Override
    @Transactional
    @Auditable(
            action = "USER_NOTIFICATION_CHANNELS_UPDATED", entityType = "USER", entityId = "#userId", comment = "#comment",
            newValue = "#result")
    public User updateNotificationChannels(UUID userId, Set<NotificationChannel> channels, String comment) {
        // No oldValue SpEL here - AuditAspect's oldValue evaluates only against this method's own
        // arguments (userId/channels/comment), never a body-local variable computed inside it; see
        // AuditAspect's Javadoc. Same documented limitation every other @Auditable usage in this
        // class already lives with (SDD_User_IAM_Services.md §4/§8).
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        // Captured before the mutation: the recorder works on the difference between the two sets,
        // and after updateNotificationChannels there is nothing left to compare against.
        Set<NotificationChannel> previousChannels = user.getNotificationChannels();
        user.updateNotificationChannels(channels);
        User saved = userRepository.save(user);
        userSearchIndex.index(saved);
        consentRecorder.recordChange(saved.getId(), previousChannels, saved.getNotificationChannels());
        return saved;
    }

    /**
     * Called by {@code LoginNotificationController} on every real login (see that class's
     * Javadoc for the full Keycloak SPI -&gt; here -&gt; ipie-communication-service chain). A
     * plain, unauthenticated-in-Keycloak-terms account (no local {@code User} row for this
     * {@code keycloakUserId}, e.g. a Keycloak-only admin account) is an expected, non-error case -
     * silently a no-op, not an exception.
     */
    @Override
    @Transactional
    public void notifyLogin(UUID keycloakUserId, Instant occurredAt, String sourceIp) {
        userRepository.findByKeycloakUserId(keycloakUserId).ifPresentOrElse(
                user -> eventPublisher.publish(UserEventType.USER_LOGGED_IN, new UserLoggedInPayload(
                        user.getId(), user.getEmail(), user.getPhoneNumber(), user.getFullName(),
                        user.getNotificationChannels(), occurredAt, sourceIp)),
                () -> LOG.debug("No local user found for keycloakUserId {} - skipping login notification", keycloakUserId));
    }

    /** Keycloak wants separate first/last names; the registration form only collects one full-name field. */
    /**
     * The tail of an identity proof number, which is all that is kept of it.
     *
     * <p>Short or absent values yield {@code null} rather than a partial mask: showing the last four
     * digits of a five-digit value would reveal most of it, and a real PAN or Aadhaar is longer than
     * that, so anything short enough to be at risk here is malformed input.
     */
    private static String lastFourDigits(String identityProofNumber) {
        if (identityProofNumber == null || identityProofNumber.length() < 8) {
            return null;
        }
        return identityProofNumber.substring(identityProofNumber.length() - 4);
    }

    private static String[] splitFullName(String fullName) {
        String trimmed = fullName.trim();
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex < 0
                ? new String[] {trimmed, trimmed}
                : new String[] {trimmed.substring(0, spaceIndex), trimmed.substring(spaceIndex + 1).trim()};
    }
}
