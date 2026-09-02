package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.ConflictException;

public class MobileNumberAlreadyExistsException extends ConflictException {

    public MobileNumberAlreadyExistsException(String mobileNumber) {
        super(UserErrorCode.MOBILE_NUMBER_ALREADY_EXISTS, "Mobile number '" + mobileNumber + "' is already registered");
    }
}
