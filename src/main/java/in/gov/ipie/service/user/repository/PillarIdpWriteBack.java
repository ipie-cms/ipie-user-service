package in.gov.ipie.service.user.repository;

import java.util.UUID;

import in.gov.ipie.service.user.domain.PillarType;

/**
 * Port for writing ipie's own user id back onto a pillar's user record - "against their respective
 * usernames so that it could be updated in their databases," per the write-back requirement.
 *
 * <p>Renamed from the concrete {@code PillarIdpAdminClient} the application layer used to inject.
 * "Admin client" described how the mock does it (Keycloak's Admin REST API against
 * {@code ibbi-mock}); a real agency exposes whatever write API it has, and the application layer
 * should not carry that assumption in the type it depends on. The HTTP implementation is
 * {@code HttpPillarIdpAdminClient}, which keeps the old name because it really is a Keycloak
 * admin call.
 *
 * <p>Callers treat this as best-effort - caught, logged, never rethrown. ipie's own
 * {@code pillar_links} row is already the authoritative record of the link; a pillar being
 * unreachable, or not yet supporting write-back at all, must never block the link itself.
 */
public interface PillarIdpWriteBack {

    /**
     * Sets the {@code ipie_id} attribute on the pillar's own user record.
     *
     * @param externalUserSub the pillar's own internal user id (the ID token's {@code sub} claim),
     *     not the {@code pillar_id} attribute value
     * @throws in.gov.ipie.service.user.integration.PillarIdpExchangeException if the read or write
     *     fails
     */
    void writeIpieId(PillarType pillarType, String externalUserSub, UUID ipieUserId);
}
