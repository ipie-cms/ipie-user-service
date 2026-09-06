package in.gov.ipie.service.user.integration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import in.gov.ipie.service.user.domain.PillarIdTokenClaims;
import in.gov.ipie.service.user.domain.PillarType;
import in.gov.ipie.service.user.repository.PillarIdpTokenClient;
import in.gov.ipie.service.user.exception.PillarIdpExchangeException;

/**
 * Authorization-code token exchange against a pillar's own OIDC token endpoint - the
 * "prove you own this external account" half of the explicit-linking handshake ({@code
 * PillarLinkService.completeLinkCallback}). No precedent elsewhere in this codebase (see
 * {@code KeycloakUserManagementClient}, which only ever does client_credentials) since this
 * service is acting as an ordinary OAuth2 client here, not calling Keycloak's own admin API - kept
 * service-local rather than promoted to common-security since only this service needs it so far.
 *
 * <p>Implements {@link PillarIdpTokenClient}; the application layer depends on that port, never
 * on this class or on {@code PillarLinkingProperties}.
 *
 * <p>Decodes the returned ID token's claims directly (no signature verification) - this is a
 * direct, confidential-client-authenticated back-channel call to the pillar's token
 * endpoint over the calling service's own TLS-terminated connection, the same trust boundary
 * {@code KeycloakUserManagementClient} already relies on for its own token responses; nothing here
 * is presented by an untrusted browser.
 */
@Component
public class HttpPillarIdpTokenClient implements PillarIdpTokenClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final PillarLinkingProperties properties;

    public HttpPillarIdpTokenClient(
            RestClient.Builder restClientBuilder, ObjectMapper objectMapper, PillarLinkingProperties properties) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Exchanges an authorization code for tokens and returns the ID token's pillar claims.
     *
     * @throws PillarIdpExchangeException if the pillar's token endpoint rejects the
     *     code, or the response carries no usable ID token
     */
    @Override
    public PillarIdTokenClaims exchangeCodeForClaims(PillarType pillarType, String code) {
        PillarLinkingProperties.Provider provider = properties.requireProvider(pillarType);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", provider.getRedirectUri());
        form.add("client_id", provider.getClientId());
        form.add("client_secret", provider.getClientSecret());

        try {
            TokenResponse response = restClient.post()
                    .uri(provider.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.idToken() == null) {
                throw new PillarIdpExchangeException(
                        pillarType + "'s token endpoint returned no id_token for this authorization code");
            }
            return decodeIdTokenClaims(response.idToken());
        } catch (HttpStatusCodeException e) {
            throw new PillarIdpExchangeException(
                    "Failed to exchange authorization code with " + pillarType + "'s token endpoint (" + e.getStatusCode() + ")", e);
        }
    }

    /**
     * Moved here from {@code PillarLinkServiceImpl} on 2026-09-05. The application layer was
     * assembling this string from {@code PillarLinkingProperties}, which is the only reason it
     * depended on that class at all - and an authorization URL's shape (which scope, how the
     * redirect URI is encoded, where {@code state} goes) is a property of the provider, not of
     * the linking policy.
     */
    @Override
    public String buildAuthorizationUrl(PillarType pillarType, UUID state) {
        PillarLinkingProperties.Provider provider = properties.requireProvider(pillarType);
        String encodedRedirectUri = URLEncoder.encode(provider.getRedirectUri(), StandardCharsets.UTF_8);
        return provider.getAuthorizationUrl()
                + "?client_id=" + provider.getClientId()
                + "&redirect_uri=" + encodedRedirectUri
                // preferred_username comes from the pillar-claims client scope's own
                // mapper (added alongside pillar_id/ipie_id), not the standard "profile"
                // scope - keeps this a single default scope, no extra scope negotiation needed.
                + "&response_type=code&scope=openid&state=" + state;
    }

    private PillarIdTokenClaims decodeIdTokenClaims(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length < 2) {
            throw new PillarIdpExchangeException("Malformed id_token (expected a JWT with 3 dot-separated parts)");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payload, PillarIdTokenClaims.class);
        } catch (Exception e) {
            throw new PillarIdpExchangeException("Failed to parse id_token claims", e);
        }
    }

    private record TokenResponse(String idToken) {

        @JsonCreator
        private TokenResponse(@JsonProperty("id_token") String idToken) {
            this.idToken = idToken;
        }
    }
}
