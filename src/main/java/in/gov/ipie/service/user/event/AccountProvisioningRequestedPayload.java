package in.gov.ipie.service.user.event;

import java.util.UUID;

/**
 * Asks ipie-iam-service to create this user's Keycloak account. Published by {@code
 * completeRegistration} instead of calling that service and waiting for it.
 *
 * <p><b>Carries no password, and must not.</b> Registration collects one, and the obvious move when
 * making provisioning asynchronous is to put it here - which would write a plaintext credential
 * into the outbox table and into every broker that relays this message, then leave it there for as
 * long as the rows are retained. That is a worse problem than the synchronous coupling this event
 * removes.
 *
 * <p>So the account is created without credentials and cannot be logged into. The user sets their
 * password through the link in the verification email, which is both the moment provisioning has
 * certainly finished and the moment they have proved control of the mailbox - the safer order
 * anyway, independent of this change.
 */
public record AccountProvisioningRequestedPayload(
        UUID userId,
        String username,
        String email,
        String firstName,
        String lastName) {
}
