package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.IpieException;

/**
 * No more codes will be issued for this registration.
 *
 * <p>This is the cap that actually bounds a guessing attack. A per-code attempt limit alone is
 * escaped by requesting a fresh code, so the count of codes ever issued is never reset - see
 * {@code V32}'s header for the arithmetic.
 *
 * <p>Recovery is deliberately not self-service: at this point the registration has either been
 * attacked or is genuinely stuck, and both want a human. Maps to <b>HTTP 422</b>.
 */
public class EmailOtpResendLimitReachedException extends IpieException {

    public EmailOtpResendLimitReachedException() {
        super(UserErrorCode.EMAIL_OTP_RESEND_LIMIT_REACHED,
                "This registration has requested too many verification codes. Please contact support.");
    }
}
