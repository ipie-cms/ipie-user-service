package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.ErrorCode;

/** Stable, service-specific error codes for the User domain (master standards doc, 5.4). */
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND,
    USERNAME_ALREADY_EXISTS,
    EMAIL_ALREADY_EXISTS,
    MOBILE_NUMBER_ALREADY_EXISTS,
    INVALID_VERIFICATION_TOKEN,

    REGISTRATION_ALREADY_COMPLETED,
    INVALID_EMAIL_OTP,
    EMAIL_OTP_ATTEMPTS_EXHAUSTED,
    EMAIL_OTP_RESEND_LIMIT_REACHED,
    EMAIL_NOT_VERIFIED;

    @Override
    public String code() {
        return name();
    }
}

