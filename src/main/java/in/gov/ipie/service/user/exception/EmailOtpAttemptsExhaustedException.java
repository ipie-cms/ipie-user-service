package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.IpieException;

/**
 * The allowance of guesses against the current email OTP is spent, and the code has been discarded.
 *
 * <p>Distinct from {@link InvalidOtpException} on purpose. That one is deliberately ambiguous about
 * whether a code was wrong or expired, because saying which would confirm that a guessed code once
 * existed. This says something different and safe to say: stop guessing, the code is gone, ask for
 * another. Without it the registrant would keep submitting a code that can no longer succeed and
 * have no idea why.
 *
 * <p>Maps to <b>HTTP 422</b> like its sibling, via {@code GlobalExceptionHandler}'s catch-all.
 */
public class EmailOtpAttemptsExhaustedException extends IpieException {

    public EmailOtpAttemptsExhaustedException() {
        super(UserErrorCode.EMAIL_OTP_ATTEMPTS_EXHAUSTED,
                "Too many incorrect attempts. This code is no longer valid - request a new one.");
    }
}
