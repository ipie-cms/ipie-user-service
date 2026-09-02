package in.gov.ipie.service.user.service;

import java.util.List;
import java.util.UUID;

import in.gov.ipie.service.user.domain.PillarLink;
import in.gov.ipie.service.user.domain.PillarLinkRequest;
import in.gov.ipie.service.user.domain.PillarType;

/**
 * Pillar-account linking use cases. See {@link PillarLinkServiceImpl} for the
 * implementation - the interface exists so callers depend on a contract rather than a concrete
 * class.
 *
 * <p>No "unlink" use case yet - {@code ACCOUNT_UNLINKED} (see {@code UserEventType}) is defined
 * and consumed by ipie-iam-service's projection sync (ADR-001), ready for whenever this interface
 * gains one; today a link, once created, can only be removed by direct database action.
 */
public interface PillarLinkService {

    /** Step 1 of explicit linking: the caller is already an authenticated ipie user. */
    PillarLinkRequest initiateLink(UUID userId, PillarType pillarType);

    /** Builds the URL {@link #initiateLink}'s caller is redirected to, once the request row exists. */
    String buildAuthorizationUrl(PillarLinkRequest request);

    /**
     * Step 2: the pillar's own IdP redirected back here after authenticating the user -
     * exchanges the code, records the authoritative link.
     */
    PillarLink completeLinkCallback(String code, UUID state);

    /** "My linked pillar accounts." */
    List<PillarLink> listLinksForUser(UUID userId);
}
