package in.gov.ipie.service.user.domain;

/**
 * The external government bodies (the pillars) an ipie user can link their account to for SSO
 * federation. Each value corresponds to a Keycloak identity-provider alias registered in the
 * {@code ipie} realm (lower-cased - see {@code deploy/keycloak/realm-export.json}'s
 * {@code identityProviders}) and to a set of endpoints under {@code
 * ipie.pillar.linking.providers.<TYPE>.*} (see {@code PillarLinkingProperties}).
 */
public enum PillarType {
    IBBI,
    NCLT,
    NCLAT,
    MCA,
    NESL
}
