package in.gov.ipie.service.user.permission;

/**
 * Permission names for the Organisation API, enforced via {@code PermissionEnforcer.require(...)}
 * / {@code @RequiresPermission} - same convention as {@link UserPermissions}.
 */
public final class OrganisationPermissions {

    public static final String ORGANISATION_READ = "ORGANISATION_READ";
    public static final String ORGANISATION_WRITE = "ORGANISATION_WRITE";

    private OrganisationPermissions() {
    }
}
