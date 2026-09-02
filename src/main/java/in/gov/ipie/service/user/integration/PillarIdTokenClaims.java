package in.gov.ipie.service.user.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The claims {@code PillarLinkService.completeLinkCallback} needs out of a pillar's
 * ID token - {@code pillarId} is the resolve-time lookup key (see the mock realm's
 * {@code pillar-id-claim} protocol mapper), {@code preferredUsername} is display/audit only,
 * {@code sub} is the pillar's own internal Keycloak user id (standard OIDC claim, no
 * custom mapper needed) - what {@code PillarIdpAdminClient.writeIpieId} targets directly,
 * added this session so the write-back step doesn't need a separate lookup-by-username call.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PillarIdTokenClaims(
        @JsonProperty("pillar_id") String pillarId,
        @JsonProperty("preferred_username") String preferredUsername,
        @JsonProperty("sub") String sub) {
}
