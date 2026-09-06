package in.gov.ipie.service.user.exception;

import java.util.UUID;

import in.gov.ipie.common.core.exception.NotFoundException;

public class OrganisationNotFoundException extends NotFoundException {

    public OrganisationNotFoundException(UUID organisationId) {
        super(OrganisationErrorCode.ORGANISATION_NOT_FOUND, "Organisation " + organisationId + " was not found");
    }
}
