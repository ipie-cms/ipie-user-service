package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.IpieException;

/**
 * Covers both an unknown token and one that has expired - the caller cannot distinguish the two
 * anyway.
 *
 * <p>Extends {@link IpieException} directly rather than {@code NotFoundException}, so
 * {@code GlobalExceptionHandler}'s catch-all maps it to <b>HTTP 422</b>. A verification token is a
 * submitted credential, not an addressable resource, so 404 was misleading - it read as "no such
 * endpoint" to callers. The deliberate unknown-vs-expired ambiguity above is preserved: the
 * response still says only "invalid or has expired", so a caller cannot probe which tokens exist.
 */
public class InvalidVerificationTokenException extends IpieException {

    public InvalidVerificationTokenException() {
        super(UserErrorCode.INVALID_VERIFICATION_TOKEN, "Verification token is invalid or has expired");
    }
}
