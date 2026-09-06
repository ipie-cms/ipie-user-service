package in.gov.ipie.service.user.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import in.gov.ipie.service.user.domain.OrganisationIdType;

/**
 * Public so {@code OrganisationRepositoryImpl} (the {@code repositoryimpl} sibling subpackage)
 * can use it - by convention, no other class should reference this interface.
 */
public interface OrganisationJpaRepository extends JpaRepository<OrganisationJpaEntity, UUID> {

    Optional<OrganisationJpaEntity> findByIdTypeAndIdValue(OrganisationIdType idType, String idValue);

    Page<OrganisationJpaEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
