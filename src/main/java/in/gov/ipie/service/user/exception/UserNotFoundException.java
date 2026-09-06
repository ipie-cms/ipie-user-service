package in.gov.ipie.service.user.exception;

import java.util.UUID;

import in.gov.ipie.common.core.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(UUID userId) {
        super(UserErrorCode.USER_NOT_FOUND, "User " + userId + " was not found");
    }
}

