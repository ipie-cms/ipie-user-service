package in.gov.ipie.service.user.integration;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import in.gov.ipie.service.user.domain.PillarType;

/**
 * Per-pillar-type OIDC endpoint/client config for the explicit account-linking handshake
 * (see {@code PillarIdpTokenClient}) - one entry per {@link PillarType}, e.g. {@code
 * ipie.pillar.linking.providers.ibbi.authorization-url}. A dedicated {@code
 * @ConfigurationProperties} class (rather than a wall of {@code @Value} constructor params, this
 * service's usual convention for a handful of scalars) because there are 4 pillar types x 5
 * properties each.
 */
@ConfigurationProperties(prefix = "ipie.pillar.linking")
public class PillarLinkingProperties {

    private final Map<PillarType, Provider> providers = new EnumMap<>(PillarType.class);

    public Map<PillarType, Provider> getProviders() {
        return providers;
    }

    public Provider requireProvider(PillarType pillarType) {
        Provider provider = providers.get(pillarType);
        if (provider == null) {
            throw new IllegalStateException(
                    "No ipie.pillar.linking.providers." + pillarType.name().toLowerCase()
                            + ".* configuration found - see application.yml");
        }
        return provider;
    }

    /** One pillar's OIDC endpoints/client, from this service's own perspective as an OAuth2 client. */
    public static class Provider {

        private String authorizationUrl;
        private String tokenUrl;
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        // The write-back call (PillarIdpAdminClient.writeIpieId) authenticates as a
        // *different*, admin-scoped client than clientId/clientSecret above - mirrors
        // KeycloakUserManagementProperties's shape, just pointed at the pillar's own realm
        // instead of ipie's own. adminBaseUrl/adminRealm default to the OIDC fields' own host/realm
        // when unset (same Keycloak instance, same realm, just a differently-privileged client).
        private String adminBaseUrl;
        private String adminRealm;
        private String adminClientId;
        private String adminClientSecret;

        public String getAuthorizationUrl() {
            return authorizationUrl;
        }

        public void setAuthorizationUrl(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        public String getAdminBaseUrl() {
            return adminBaseUrl != null ? adminBaseUrl : deriveBaseUrlFromTokenUrl();
        }

        public void setAdminBaseUrl(String adminBaseUrl) {
            this.adminBaseUrl = adminBaseUrl;
        }

        public String getAdminRealm() {
            return adminRealm;
        }

        public void setAdminRealm(String adminRealm) {
            this.adminRealm = adminRealm;
        }

        public String getAdminClientId() {
            return adminClientId;
        }

        public void setAdminClientId(String adminClientId) {
            this.adminClientId = adminClientId;
        }

        public String getAdminClientSecret() {
            return adminClientSecret;
        }

        public void setAdminClientSecret(String adminClientSecret) {
            this.adminClientSecret = adminClientSecret;
        }

        private String deriveBaseUrlFromTokenUrl() {
            // tokenUrl looks like http://host:port/realms/{realm}/protocol/openid-connect/token -
            // the admin API lives at http://host:port/admin/realms/{realm}/... on the same host.
            int realmsIndex = tokenUrl.indexOf("/realms/");
            return realmsIndex < 0 ? tokenUrl : tokenUrl.substring(0, realmsIndex);
        }
    }
}
