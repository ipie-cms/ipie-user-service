package in.gov.ipie.service.user.repository;

import java.util.Set;
import java.util.UUID;

/** Reads the organisation tree that {@code VisibilityScope}'s hierarchy axis is built from. */
public interface OrganisationHierarchyRepository {

    /**
     * Every node at or beneath {@code rootId}, the root included.
     *
     * <p>Resolved as one recursive query rather than by walking the tree in Java, because the walk
     * costs a round trip per level and the depth is not bounded by anything the schema enforces.
     * Returning ids (not entities) keeps the result small enough to hand straight to an {@code IN}
     * clause on the user query, which is what makes the visibility filter a {@code WHERE} rather
     * than a fetch-then-filter - the distinction 10.4 insists on, because filtering after the fact
     * breaks pagination and leaks through row totals.
     */
    Set<UUID> subtreeOf(UUID rootId);

    /**
     * Every node at or beneath any of {@code rootIds}. For the backend that cannot express the
     * relation as a subquery and must have the ids in hand.
     */
    default Set<UUID> expand(Set<UUID> rootIds) {
        java.util.Set<UUID> all = new java.util.HashSet<>();
        for (UUID root : rootIds) {
            all.addAll(subtreeOf(root));
        }
        return all;
    }
}
