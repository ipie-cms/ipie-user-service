package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.ErrorCode;

/** Stable, service-specific error codes for the pillar-link domain (master standards doc, 5.4). */
public enum PillarLinkErrorCode implements ErrorCode {
    STAKEHOLDER_ALREADY_LINKED,
    LINK_REQUEST_NOT_FOUND_OR_EXPIRED,
    STAKEHOLDER_IDP_EXCHANGE_FAILED;

    @Override
    public String code() {
        return name();
    }
}
