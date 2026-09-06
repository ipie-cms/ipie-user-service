package in.gov.ipie.service.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.service.user.domain.PillarLink;
import in.gov.ipie.service.user.domain.PillarType;

/** Domain-owned port for {@link PillarLink} persistence - the JPA adapter lives in infrastructure. */
public interface PillarLinkRepository {

    PillarLink save(PillarLink link);

    /** The resolve-time lookup key the Keycloak SPI's {@code resolve} call ultimately depends on. */
    Optional<PillarLink> findByPillarTypeAndExternalPillarId(PillarType pillarType, String externalPillarId);

    boolean existsByUserIdAndPillarType(UUID userId, PillarType pillarType);

    List<PillarLink> findAllByUserId(UUID userId);
}
