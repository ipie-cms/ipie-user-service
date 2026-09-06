package in.gov.ipie.service.user.repository;

import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.domain.OrganisationIdType;

/**
 * Domain-owned port for Organisation persistence - same layering reasoning as {@link
 * UserRepository}.
 */
public interface OrganisationRepository {

    Organisation save(Organisation organisation);

    Optional<Organisation> findById(UUID id);

    /** The dedup lookup {@code OrganisationService#findOrCreate} checks before inserting. */
    Optional<Organisation> findByIdTypeAndIdValue(OrganisationIdType idType, String idValue);

    PageResult<Organisation> searchByName(String name, PageRequest pageRequest);
}
