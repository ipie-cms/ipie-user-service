package in.gov.ipie.service.user.service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.command.CompleteRegistrationCommand;
import in.gov.ipie.service.user.command.ConfirmEmailOtpCommand;
import in.gov.ipie.service.user.command.CreateRegistrationCommand;
import in.gov.ipie.service.user.command.CreateUserCommand;
import in.gov.ipie.service.user.command.SaveRegistrationDraftCommand;
import in.gov.ipie.service.user.command.UpdateUserCommand;
import in.gov.ipie.service.user.domain.NotificationChannel;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.VisibilityScope;

/**
 * User CRUD and self-registration use cases. See {@link UserServiceImpl} for the implementation -
 * the interface exists so callers depend on a contract rather than a concrete class.
 */
public interface UserService {

    User createUser(CreateUserCommand command);

    User getUser(UUID userId);

    PageResult<User> searchUsers(UserSearchCriteria criteria, VisibilityScope visibleTo, PageRequest pageRequest);

    /** Keyset ("seek") variant of {@link #searchUsers}; see {@code CursorPageRequest}'s Javadoc. */
    CursorPageResult<User> searchUsersAfter(UserSearchCriteria criteria, VisibilityScope visibleTo, CursorPageRequest pageRequest);

    User updateUser(UpdateUserCommand command);

    /** {@code comment} is the human-supplied reason for the audit trail - may be {@code null}. */
    User deactivateUser(UUID userId, String comment);

    /** {@code comment} is the human-supplied reason for the audit trail - may be {@code null}. */
    User reactivateUser(UUID userId, String comment);

    /** Step 1 of self-registration: capture mobile + email only, no Keycloak account yet. */
    User createRegistration(CreateRegistrationCommand command);

    /**
     * Step 2: provisions the Keycloak account (the user can log in from this point on) and moves
     * to {@code UNVERIFIED} - publishes {@code UserEventType#USER_REGISTRATION_COMPLETED} so
     * ipie-communication-service can email the pillar admin.
     */
    User completeRegistration(CompleteRegistrationCommand command);

    /**
     * The registration wizard's "Save Draft" action - partial, repeatable update onto a still-
     * {@code PRE_REGISTRATION} row (and its {@code Organisation}, if Entity details are present).
     */
    User saveRegistrationDraft(SaveRegistrationDraftCommand command);

    /**
     * "SEND OTP" next to the registration wizard's Email field - generates and emails a fresh
     * code (see {@code UserEventType#REGISTRATION_EMAIL_OTP_REQUESTED}), overwriting any earlier,
     * unconfirmed one.
     */
    User requestEmailOtp(UUID registrationId);

    /** The registrant submitted the code emailed by {@link #requestEmailOtp} - gates {@link #completeRegistration}. */
    User confirmEmailOtp(ConfirmEmailOtpCommand command);

    /** The pillar admin followed the emailed verification link. */
    /**
     * ipie-iam-service has created this user's Keycloak account. Stamps the id, moves the
     * registration out of {@code PROVISIONING}, and only then publishes
     * {@code USER_REGISTRATION_COMPLETED} - the event that sends the verification email, held back
     * until the account it points at exists.
     */
    User accountProvisioned(UUID userId, UUID keycloakUserId);

    User verifyByToken(String token);

    /** Resolves the authenticated caller (JWT {@code sub}) for {@code GET /api/v1/users/me}. */
    User getCurrentUser(UUID keycloakUserId);

    /**
     * Sets or clears (pass {@code null}) which {@code Organisation} this user is affiliated with
     * (FRS 1.1.1) - its own explicit, audited use case rather than folded into {@link
     * #updateUser}, since affiliation is a distinct business action, not a profile-field edit.
     */
    User affiliateWithOrganisation(UUID userId, UUID organisationId, String comment);

    /** {@code comment} is the human-supplied reason for the audit trail - may be {@code null}. */
    User updateNotificationChannels(UUID userId, Set<NotificationChannel> channels, String comment);

    /**
     * Publishes {@code UserEventType#USER_LOGGED_IN} for the local user matching {@code
     * keycloakUserId}, if one exists - see {@code LoginNotificationController}'s Javadoc for the
     * full call chain from a real Keycloak login.
     */
    void notifyLogin(UUID keycloakUserId, Instant occurredAt, String sourceIp);
}
