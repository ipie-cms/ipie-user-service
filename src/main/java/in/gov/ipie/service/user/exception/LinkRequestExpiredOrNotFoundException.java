package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.NotFoundException;

/** Raised on the OIDC redirect callback when {@code state} doesn't match a still-pending link request. */
public class LinkRequestExpiredOrNotFoundException extends NotFoundException {

    public LinkRequestExpiredOrNotFoundException() {
        super(
                PillarLinkErrorCode.LINK_REQUEST_NOT_FOUND_OR_EXPIRED,
                "The pillar-link request was not found, already used, or has expired");
    }
}
