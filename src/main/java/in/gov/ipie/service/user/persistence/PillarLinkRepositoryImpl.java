package in.gov.ipie.service.user.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import in.gov.ipie.common.persistence.IntegrityViolations;
import in.gov.ipie.service.user.domain.PillarLink;
import in.gov.ipie.service.user.domain.PillarType;
import in.gov.ipie.service.user.repository.PillarLinkRepository;

/**
 * Infrastructure-layer adapter implementing {@link PillarLinkRepository} on Spring Data JPA -
 * the only class allowed to know about the JPA entity/repository (same convention as {@code
 * UserRepositoryImpl}). {@link PillarLink} is create-only (no update path), so unlike {@code
 * UserRepositoryImpl#save} there is no "existing id" branch here.
 */
@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PillarLinkRepositoryImpl implements PillarLinkRepository {

    /**
     * Two different uniqueness rules that used to share one message: the same pillar identity being
     * claimed by a second ipie user, and one user linking the same pillar twice. Those are different
     * problems for whoever hits them - the first needs the other account investigated, the second is
     * simply already done.
     */
    static final IntegrityViolations VIOLATIONS = IntegrityViolations.forTable()
            .primaryKey("pillar_links_pkey")
            .conflict("uq_pillar_links_type_external_id",
                    "This pillar account is already linked to another ipie user")
            .conflict("uq_pillar_links_user_type", "This user already has a link to that pillar")
            .build();

    private final PillarLinkJpaRepository jpaRepository;
    private final PillarLinkPersistenceMapper mapper;

    @Override
    public PillarLink save(PillarLink link) {
        try {
            return mapper.toDomain(jpaRepository.save(mapper.toNewEntity(link)));
        } catch (DataIntegrityViolationException e) {
            throw VIOLATIONS.translate(e);
        }
    }

    @Override
    public Optional<PillarLink> findByPillarTypeAndExternalPillarId(
            PillarType pillarType, String externalPillarId) {
        return jpaRepository.findByPillarTypeAndExternalPillarId(pillarType, externalPillarId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndPillarType(UUID userId, PillarType pillarType) {
        return jpaRepository.existsByUserIdAndPillarType(userId, pillarType);
    }

    @Override
    public List<PillarLink> findAllByUserId(UUID userId) {
        return jpaRepository.findAllByUserId(userId).stream().map(mapper::toDomain).toList();
    }
}
