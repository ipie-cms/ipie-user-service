package in.gov.ipie.service.user.permission;

/**
 * Permission names for the User API, enforced via {@code PermissionEnforcer.require(...)}.
 * Business logic must never check role names directly (master standards doc, 5.5) - only these
 * permission constants, resolved from the caller's JWT by common-security.
 */
public final class UserPermissions {

    public static final String USER_READ = "USER_READ";
    public static final String USER_WRITE = "USER_WRITE";
    public static final String USER_DELETE = "USER_DELETE";

    /** Gates {@code POST /internal/logins/notify} - granted only to ipie-keycloak-spi's own service account. */
    public static final String LOGIN_NOTIFY = "LOGIN_NOTIFY";

    private UserPermissions() {
    }
}
