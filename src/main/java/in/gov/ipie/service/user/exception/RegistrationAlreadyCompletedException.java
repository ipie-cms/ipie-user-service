package in.gov.ipie.service.user.exception;

import java.util.UUID;

import in.gov.ipie.common.core.exception.ConflictException;

public class RegistrationAlreadyCompletedException extends ConflictException {

    public RegistrationAlreadyCompletedException(UUID registrationId) {
        super(UserErrorCode.REGISTRATION_ALREADY_COMPLETED, "Registration " + registrationId + " was already completed");
    }
}
