package in.gov.ipie.service.user.domain;

import java.util.UUID;

/**
 * One selectable dropdown option from a database-backed "closed list that can grow" lookup table
 * (professional roles, legal representative types, ...) - see {@code LookupJpaEntity}'s Javadoc.
 * Shared across every such catalogue since the shape is identical and carries no per-catalogue
 * behavior; {@code RegistrationLookupServiceImpl} is what keeps the catalogues themselves
 * separate tables.
 */
public record LookupOption(UUID id, String code, String label) {
}
