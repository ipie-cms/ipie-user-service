package in.gov.ipie.service.user.exception;

import java.util.UUID;

import in.gov.ipie.common.core.exception.ConflictException;

/** Raised by {@code completeRegistration} - the registrant must confirm the email OTP first (see {@code User#confirmEmailOtp}). */
public class EmailNotVerifiedException extends ConflictException {

    public EmailNotVerifiedException(UUID registrationId) {
        super(UserErrorCode.EMAIL_NOT_VERIFIED, "Registration " + registrationId + " has not confirmed its email OTP yet");
    }
}
