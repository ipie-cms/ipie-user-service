package in.gov.ipie.service.user.service;

import java.util.UUID;

import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.command.CreateOrganisationCommand;
import in.gov.ipie.service.user.command.UpdateOrganisationCommand;
import in.gov.ipie.service.user.domain.Organisation;

/**
 * Organisation use cases. See {@link OrganisationServiceImpl} for the implementation - the
 * interface exists so callers depend on a contract rather than a concrete class, same convention
 * as {@link UserService}.
 */
public interface OrganisationService {

    /**
     * The one write path for creating an organisation: looks up {@code (idType, idValue)} first
     * and returns the existing row if found, otherwise creates a new one - "system should ensure
     * that no duplication of entity records happen" (FRS 1.1.1). Never throws a conflict for an
     * existing organisation; the database's unique constraint is the backstop, not the primary
     * enforcement mechanism.
     */
    Organisation findOrCreate(CreateOrganisationCommand command);

    Organisation getOrganisation(UUID organisationId);

    PageResult<Organisation> searchOrganisations(String name, PageRequest pageRequest);

    Organisation updateOrganisation(UpdateOrganisationCommand command);
}
