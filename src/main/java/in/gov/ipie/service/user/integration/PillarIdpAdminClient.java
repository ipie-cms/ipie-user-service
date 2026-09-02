package in.gov.ipie.service.user.integration;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import in.gov.ipie.service.user.domain.PillarType;

/**
 * Writes ipie's own user id back onto a pillar's own user record - "against their
 * respective usernames so that it could be updated in their databases," per the write-back
 * requirement. Separate from {@link PillarIdpTokenClient} since it's a different concern: an
 * admin-style write against the *pillar's* Keycloak realm (client-credentials grant as
 * {@code ipie-admin-writeback}), not an OIDC login flow. For the mock, this proves the concept via
 * Keycloak's own Admin REST API on {@code ibbi-mock} - a real agency without a Keycloak-backed
 * system of its own would instead need whatever write API it actually exposes, with this class's
 * shape as the template.
 *
 * <p>Calls from {@link in.gov.ipie.service.user.service.PillarLinkService} are
 * deliberately best-effort (caught, logged, never thrown further) - ipie's own {@code
 * pillar_links} row is already the authoritative record of the link; a pillar being
 * unreachable or not yet supporting the write-back must never block the link itself.
 */
@Component
public class PillarIdpAdminClient {

    private final RestClient restClient;
    private final PillarLinkingProperties properties;

    public PillarIdpAdminClient(RestClient.Builder restClientBuilder, PillarLinkingProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    /**
     * Sets the {@code ipie_id} attribute on the pillar's own user record.
     *
     * <p>Read-modify-write: Keycloak's user-update endpoint treats a supplied {@code attributes}
     * map as a full replacement, not a merge - found the hard way, when this call wiped out the
     * same user's {@code pillar_id} attribute the first time this was tested end-to-end. So
     * this fetches the user's current attributes first and only adds/overwrites {@code ipie_id}
     * within them.
     *
     * @param externalUserSub the pillar's own internal user id (the ID token's {@code
     *     sub} claim - see {@link PillarIdTokenClaims}), not {@code externalPillarId}
     *     (the {@code pillar_id} attribute value) - Keycloak's Admin API addresses users by
     *     their internal id, not by a custom attribute
     * @throws PillarIdpExchangeException if the admin read or write fails
     */
    @SuppressWarnings("unchecked")
    public void writeIpieId(PillarType pillarType, String externalUserSub, UUID ipieUserId) {
        PillarLinkingProperties.Provider provider = properties.requireProvider(pillarType);
        String token = clientCredentialsToken(provider);
        String userUrl = provider.getAdminBaseUrl() + "/admin/realms/" + provider.getAdminRealm() + "/users/" + externalUserSub;

        try {
            Map<String, Object> currentUser = restClient.get()
                    .uri(userUrl)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(Map.class);
            Map<String, Object> attributes = currentUser != null && currentUser.get("attributes") instanceof Map
                    ? new java.util.HashMap<>((Map<String, Object>) currentUser.get("attributes"))
                    : new java.util.HashMap<>();
            attributes.put("ipie_id", List.of(ipieUserId.toString()));

            restClient.put()
                    .uri(userUrl)
                    .headers(headers -> headers.setBearerAuth(token))
                    .body(Map.of("attributes", attributes))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException e) {
            throw new PillarIdpExchangeException(
                    "Failed to write ipie_id back to " + pillarType + "'s user " + externalUserSub
                            + " (" + e.getStatusCode() + ")", e);
        }
    }

    private String clientCredentialsToken(PillarLinkingProperties.Provider provider) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", provider.getAdminClientId());
        form.add("client_secret", provider.getAdminClientSecret());

        try {
            TokenResponse response = restClient.post()
                    .uri(provider.getAdminBaseUrl() + "/realms/" + provider.getAdminRealm() + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new PillarIdpExchangeException("No access_token in admin client-credentials response");
            }
            return response.accessToken();
        } catch (HttpStatusCodeException e) {
            throw new PillarIdpExchangeException(
                    "Failed to obtain an admin client-credentials token for " + provider.getAdminClientId()
                            + " (" + e.getStatusCode() + ")", e);
        }
    }

    private record TokenResponse(String accessToken) {

        @JsonCreator
        private TokenResponse(@JsonProperty("access_token") String accessToken) {
            this.accessToken = accessToken;
        }
    }
}
