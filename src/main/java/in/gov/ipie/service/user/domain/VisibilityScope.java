package in.gov.ipie.service.user.domain;

import java.util.Set;
import java.util.UUID;

/**
 * What one caller is allowed to see, resolved once per request and then applied as a {@code WHERE}
 * clause (see {@code UserSpecifications#visibleTo}).
 *
 * <p>Two axes, both optional, because most principals sit on one and not the other (programme,
 * 2026-08-17):
 *
 * <ul>
 *   <li><b>Hierarchy</b> - the organisation nodes beneath the caller's own, inclusive. An entity
 *       admin roots a tree; the users they invite carry the entity as their node. A parent sees
 *       everything below it and siblings see nothing of each other, which is what {@code
 *       organisationIds} being a *subtree* rather than a single id expresses.
 *   <li><b>Pillar</b> - the pillar the caller administers. An IBBI pillar admin sees every principal
 *       IBBI validated, wherever they sit in any hierarchy, because an insolvency professional
 *       belongs to no organisation at all.
 * </ul>
 *
 * <p>The two are a union, not an intersection: a caller who is both an entity admin and a pillar
 * admin sees their subtree *and* their pillar. Intersecting them would hide the IPs that pillar
 * admins exist to administer, since those users have no organisation to intersect with.
 *
 * <p>{@code unrestricted} is the platform operator, and is a distinct state rather than "every
 * organisation and every pillar". Enumerating everything would silently start excluding rows the
 * moment a user exists with neither axis set - which is most seeded accounts today.
 *
 * <p>{@code selfUserId} is always present and always visible. A user with no administrative role
 * sees exactly one record: their own. That is the floor, not an empty result.
 */
public record VisibilityScope(boolean unrestricted, UUID selfUserId, Set<UUID> hierarchyRootIds, Set<String> pillarScopes) {

    public VisibilityScope {
        hierarchyRootIds = Set.copyOf(hierarchyRootIds);
        pillarScopes = Set.copyOf(pillarScopes);
    }

    /** The platform operator: no predicate is added at all. */
    public static VisibilityScope unrestricted(UUID selfUserId) {
        return new VisibilityScope(true, selfUserId, Set.of(), Set.of());
    }

    /** A caller with no administrative reach - visible to themselves and nobody else. */
    public static VisibilityScope selfOnly(UUID selfUserId) {
        return new VisibilityScope(false, selfUserId, Set.of(), Set.of());
    }

    public boolean isSelfOnly() {
        return !unrestricted && hierarchyRootIds.isEmpty() && pillarScopes.isEmpty();
    }
}
