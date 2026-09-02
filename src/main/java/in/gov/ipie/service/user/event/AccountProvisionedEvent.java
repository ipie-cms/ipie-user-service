package in.gov.ipie.service.user.event;

import java.util.UUID;

/**
 * This service's view of ipie-iam-service's {@code ACCOUNT_PROVISIONED} - the reply to the
 * provisioning this service requested when a registration was completed. Each service owns its copy
 * of a contract rather than sharing a class across a repository boundary, the same way
 * {@code UserVerifiedEvent} is mirrored in ipie-iam-service.
 */
public record AccountProvisionedEvent(UUID userId, UUID keycloakUserId) {
}
