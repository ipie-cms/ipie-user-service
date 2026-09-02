package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.IpieException;

/**
 * Covers both a wrong code and one that has expired - the caller cannot distinguish the two anyway
 * (mirrors {@link InvalidVerificationTokenException}).
 *
 * <p>Extends {@link IpieException} directly rather than {@code NotFoundException}, so
 * {@code GlobalExceptionHandler}'s catch-all maps it to <b>HTTP 422</b>. A submitted OTP is a
 * rejected credential, not a missing resource: the registration it belongs to exists, and 404 would
 * both mislead API consumers and be indistinguishable from a mistyped URL. The deliberate
 * wrong-vs-expired ambiguity above is preserved - the response still says only "invalid or has
 * expired", so nothing here confirms whether a given code was ever real.
 */
public class InvalidOtpException extends IpieException {

    public InvalidOtpException() {
        super(UserErrorCode.INVALID_EMAIL_OTP, "OTP code is invalid or has expired");
    }
}
