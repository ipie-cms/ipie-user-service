package in.gov.ipie.service.user.domain;

/** Lifecycle of a {@link PillarLinkRequest} - the short-lived initiate/callback handshake row. */
public enum PillarLinkRequestStatus {
    PENDING,
    COMPLETED,
    EXPIRED
}
