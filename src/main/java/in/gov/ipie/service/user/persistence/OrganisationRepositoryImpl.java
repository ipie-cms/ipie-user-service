package in.gov.ipie.service.user.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import in.gov.ipie.common.persistence.IntegrityViolations;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.domain.Organisation;
import in.gov.ipie.service.user.domain.OrganisationIdType;
import in.gov.ipie.service.user.exception.OrganisationNotFoundException;
import in.gov.ipie.service.user.repository.OrganisationRepository;

/**
 * Infrastructure-layer adapter implementing the domain-owned {@link OrganisationRepository} port
 * on top of Spring Data JPA - same shape as {@code UserRepositoryImpl}. The only class in this
 * service allowed to know about {@link OrganisationJpaEntity} and {@link OrganisationJpaRepository}.
 */
@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class OrganisationRepositoryImpl implements OrganisationRepository {

    static final IntegrityViolations VIOLATIONS = IntegrityViolations.forTable()
            .primaryKey("organisations_pkey")
            .conflict("uq_organisations_id_type_value",
                    "An organisation with the same id type and value already exists")
            .conflict("uq_organisations_user_id", "That principal is already an organisation")
            .build();

    private final OrganisationJpaRepository jpaRepository;
    private final OrganisationPersistenceMapper mapper;

    /**
     * Evicts {@code findByIdTypeAndIdValue}'s cache entry for this organisation's own (immutable)
     * id-type/id-value - a harmless no-op for the create path (nothing was ever cached there
     * before creation, since {@link #findByIdTypeAndIdValue}'s own {@code @Cacheable} never
     * caches a not-found result), and the correct invalidation for the update path.
     */
    @Override
    @CacheEvict(cacheNames = "organisations-by-id", key = "#organisation.idType.name() + ':' + #organisation.idValue")
    public Organisation save(Organisation organisation) {
        try {
            if (organisation.getId() == null) {
                OrganisationJpaEntity saved = jpaRepository.save(mapper.toNewEntity(organisation));
                return mapper.toDomain(saved);
            }

            OrganisationJpaEntity entity = jpaRepository.findById(organisation.getId())
                    .orElseThrow(() -> new OrganisationNotFoundException(organisation.getId()));
            mapper.copyMutableFieldsOnto(organisation, entity);
            return mapper.toDomain(jpaRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw VIOLATIONS.translate(e);
        }
    }

    @Override
    public Optional<Organisation> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * {@code unless} is required, not optional: without it, an about-to-be-created organisation's
     * not-found lookup would be cached, and a subsequent {@code findOrCreate} call for the same
     * id-type/id-value would keep seeing that stale "not found" and attempt a duplicate create
     * (caught by the database's own unique constraint, but not the clean dedup this cache is
     * meant to speed up, not break).
     */
    @Override
    @Cacheable(cacheNames = "organisations-by-id", key = "#idType.name() + ':' + #idValue",
            unless = "#result == null || #result.isEmpty()")
    public Optional<Organisation> findByIdTypeAndIdValue(OrganisationIdType idType, String idValue) {
        return jpaRepository.findByIdTypeAndIdValue(idType, idValue).map(mapper::toDomain);
    }

    @Override
    public PageResult<Organisation> searchByName(String name, PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        Page<OrganisationJpaEntity> page = jpaRepository.findByNameContainingIgnoreCase(name == null ? "" : name, pageable);
        List<Organisation> content = page.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
