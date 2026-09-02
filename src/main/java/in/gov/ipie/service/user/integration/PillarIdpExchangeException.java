package in.gov.ipie.service.user.integration;

import in.gov.ipie.common.core.exception.IpieException;
import in.gov.ipie.service.user.exception.PillarLinkErrorCode;

/**
 * An authorization-code token exchange against a pillar's own IdP failed. Extends
 * {@link IpieException} (master standards doc binding rules, Section 13 - every {@code
 * *Exception} class must, ArchUnit-enforced) rather than a raw {@code RuntimeException}; falls
 * through to {@code GlobalExceptionHandler}'s generic {@code IpieException} handler (HTTP 422) if
 * it ever escapes uncaught, though in practice {@code PillarLinkController#callback} always
 * catches it and redirects with a failure flag instead.
 */
public class PillarIdpExchangeException extends IpieException {

    public PillarIdpExchangeException(String message) {
        super(PillarLinkErrorCode.STAKEHOLDER_IDP_EXCHANGE_FAILED, message);
    }

    public PillarIdpExchangeException(String message, Throwable cause) {
        super(PillarLinkErrorCode.STAKEHOLDER_IDP_EXCHANGE_FAILED, message, cause);
    }
}
