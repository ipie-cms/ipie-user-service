package in.gov.ipie.service.user.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import in.gov.ipie.service.user.domain.PillarType;

/** Public so {@code PillarLinkRepositoryImpl} (the {@code repositoryimpl} sibling subpackage) can use it. */
public interface PillarLinkJpaRepository extends JpaRepository<PillarLinkJpaEntity, UUID> {

    Optional<PillarLinkJpaEntity> findByPillarTypeAndExternalPillarId(
            PillarType pillarType, String externalPillarId);

    boolean existsByUserIdAndPillarType(UUID userId, PillarType pillarType);

    List<PillarLinkJpaEntity> findAllByUserId(UUID userId);
}
