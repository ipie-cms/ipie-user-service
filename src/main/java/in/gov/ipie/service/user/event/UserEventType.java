package in.gov.ipie.service.user.event;

/** Business event names this service publishes, and the contract version they are published at. */
public enum UserEventType {
    USER_CREATED,
    USER_UPDATED,
    USER_DEACTIVATED,
    USER_REACTIVATED,
    /** Step 2 of self-registration completed - consumed by ipie-communication-service to email the pillar admin. */
    /**
     * Asks ipie-iam-service to create the Keycloak account. Emitted the moment the registration
     * form is complete; USER_REGISTRATION_COMPLETED below follows only once that account exists,
     * so the verification email is never sent to someone who cannot yet set a password.
     */
    ACCOUNT_PROVISIONING_REQUESTED,

    USER_REGISTRATION_COMPLETED,
    /**
     * The registration wizard's "Email SEND OTP" was clicked - consumed by
     * ipie-communication-service to email the code to the registrant's own address.
     */
    REGISTRATION_EMAIL_OTP_REQUESTED,
    /** Pillar admin followed the verification link - consumed by ipie-iam-service to auto-assign the default role. */
    USER_VERIFIED,
    /**
     * A pillar account was linked (explicit handshake completed) - consumed by
     * ipie-iam-service to upsert its {@code pillar_resolution} read projection (ADR-001,
     * "Placement of pillar_links Data and /resolve Endpoint").
     */
    ACCOUNT_LINKED,
    /** The counterpart to {@link #ACCOUNT_LINKED} - a link was removed. */
    ACCOUNT_UNLINKED,
    /**
     * A real login happened (published by {@code LoginNotificationController}, called by the
     * Keycloak Event Listener SPI - see that controller's Javadoc) - consumed by
     * ipie-communication-service to notify the account owner over their opted channel(s).
     */
    USER_LOGGED_IN;

    public static final int CONTRACT_VERSION = 1;
}

