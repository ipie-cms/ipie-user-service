package in.gov.ipie.service.user.domain;

/**
 * A user's visibility status. Rows are never deleted - "delete" flips this to {@code INACTIVE}
 * instead (master standards doc, section 8: "No hard deletes").
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE
}

