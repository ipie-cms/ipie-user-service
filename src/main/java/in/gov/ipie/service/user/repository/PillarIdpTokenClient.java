package in.gov.ipie.service.user.repository;

import java.util.UUID;

import in.gov.ipie.service.user.domain.PillarIdTokenClaims;
import in.gov.ipie.service.user.domain.PillarType;

/**
 * Port for the OIDC half of the explicit pillar-linking handshake: build the authorization URL the
 * user is sent to, then exchange the code the pillar redirects back with.
 *
 * <p>Was a concrete {@code @Component} that {@code PillarLinkServiceImpl} injected directly, along
 * with {@code PillarLinkingProperties} so it could assemble the authorization URL itself. Both are
 * gone from the application layer: a URL built out of one provider's {@code authorizationUrl},
 * {@code clientId}, {@code redirectUri} and scope conventions is that provider's business, not the
 * linking policy's. The HTTP implementation is {@code HttpPillarIdpTokenClient} - naming follows
 * {@code UserSearchIndex}/{@code ElasticsearchUserSearchIndex}, port plain, adapter prefixed by
 * its technology.
 *
 * <p>Kept separate from {@link PillarIdpWriteBack} because the two authenticate as different
 * clients against different endpoints, and one is best-effort while the other is not.
 */
public interface PillarIdpTokenClient {

    /**
     * The pillar's authorization endpoint with this service's client, redirect URI and the link
     * request's id as {@code state}.
     */
    String buildAuthorizationUrl(PillarType pillarType, UUID state);

    /**
     * Exchanges an authorization code for tokens and returns the ID token's pillar claims.
     *
     * @throws in.gov.ipie.service.user.integration.PillarIdpExchangeException if the pillar's
     *     token endpoint rejects the code, or the response carries no usable ID token
     */
    PillarIdTokenClaims exchangeCodeForClaims(PillarType pillarType, String code);
}
