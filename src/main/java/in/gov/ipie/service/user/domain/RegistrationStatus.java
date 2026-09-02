package in.gov.ipie.service.user.domain;

/**
 * The registration lifecycle, independent of {@link UserStatus} (account enabled/disabled) - a
 * {@code VERIFIED} user can still later be deactivated by an admin, and the two axes are tracked
 * separately.
 */
public enum RegistrationStatus {

    /** Mobile number + email captured; no Keycloak account exists yet. */
    PRE_REGISTRATION,

    /**
     * The registration form is complete and account creation has been requested, but the Keycloak
     * account does not exist yet.
     *
     * <p>This state exists because provisioning moved off the request path: {@code
     * completeRegistration} publishes an event and returns, rather than calling ipie-iam-service and
     * waiting. Registration therefore no longer fails because Keycloak is busy or briefly
     * unavailable - but it does mean there is a window where the row exists and the account does
     * not, and a state nobody can see is a state support cannot explain. A registration sitting
     * here means the provisioning event has not been processed: check {@code ipie.events.dlq}.
     */
    PROVISIONING,

    /** Keycloak account created (the user can set a password and log in), pending pillar-admin verification. */
    UNVERIFIED,

    /** Verified by a pillar admin - the dashboard shows role-based content. */
    VERIFIED
}
