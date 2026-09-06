package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.ErrorCode;

/** Stable, service-specific error codes for the Organisation domain (master standards doc, 5.4). */
public enum OrganisationErrorCode implements ErrorCode {
    ORGANISATION_NOT_FOUND;

    @Override
    public String code() {
        return name();
    }
}
