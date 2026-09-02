package in.gov.ipie.service.user.event;

import java.util.UUID;

/**
 * Payload for {@link UserEventType#USER_VERIFIED} - carries both id shapes ipie-iam-service needs:
 * {@code userId} (this service's id, the key {@code user_roles} is keyed by) and {@code
 * keycloakUserId} (needed to sync the realm-role assignment to Keycloak).
 */
public record UserVerifiedPayload(UUID userId, UUID keycloakUserId, String email) {
}
