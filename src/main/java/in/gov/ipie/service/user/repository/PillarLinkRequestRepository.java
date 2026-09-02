package in.gov.ipie.service.user.repository;

import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.service.user.domain.PillarLinkRequest;

/** Domain-owned port for {@link PillarLinkRequest} persistence. */
public interface PillarLinkRequestRepository {

    PillarLinkRequest save(PillarLinkRequest request);

    Optional<PillarLinkRequest> findById(UUID id);
}
