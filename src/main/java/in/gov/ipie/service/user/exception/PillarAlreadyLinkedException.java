package in.gov.ipie.service.user.exception;

import in.gov.ipie.common.core.exception.ConflictException;
import in.gov.ipie.service.user.domain.PillarType;

public class PillarAlreadyLinkedException extends ConflictException {

    public PillarAlreadyLinkedException(PillarType pillarType) {
        super(
                PillarLinkErrorCode.STAKEHOLDER_ALREADY_LINKED,
                "A " + pillarType + " account is already linked to this user, or that external account "
                        + "is already linked to a different user");
    }
}
