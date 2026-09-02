package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.ConflictException;

public class EmailAlreadyExistsException extends ConflictException {

    public EmailAlreadyExistsException(String email) {
        super(UserErrorCode.EMAIL_ALREADY_EXISTS, "Email '" + email + "' is already registered");
    }
}

