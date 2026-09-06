package in.gov.ipie.service.user.persistence;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * The transitive closure of the organisation tree - one row per ancestor/descendant pair, maintained
 * by a trigger (user-service {@code V12}).
 *
 * <p>Mapped only so the visibility predicate can be expressed as a *subquery* rather than as an
 * {@code IN} list built in Java. That distinction is the whole point: resolving descendants into a
 * collection and passing them back down makes the query text grow with the size of the caller's org
 * chart, while a subquery leaves the expansion in the database where the index is.
 *
 * <p>Read-only from the application's side. Nothing here writes it; writing it from two places is
 * how a closure table drifts from the tree it describes, and a closure table that disagrees with
 * {@code parent_id} silently returns the wrong rows to an administrator.
 */
@Entity
@Table(name = "organisation_closure")
@IdClass(OrganisationClosureJpaEntity.Key.class)
public class OrganisationClosureJpaEntity {

    @Id
    @Column(name = "ancestor_id", nullable = false)
    private UUID ancestorId;

    @Id
    @Column(name = "descendant_id", nullable = false)
    private UUID descendantId;

    @Column(name = "depth", nullable = false)
    private int depth;

    public UUID getAncestorId() {
        return ancestorId;
    }

    public UUID getDescendantId() {
        return descendantId;
    }

    public int getDepth() {
        return depth;
    }

    /** Composite key: the pair is the identity, and there is no surrogate to add. */
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        private UUID ancestorId;
        private UUID descendantId;

        public Key() {
        }

        public Key(UUID ancestorId, UUID descendantId) {
            this.ancestorId = ancestorId;
            this.descendantId = descendantId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Key key)) {
                return false;
            }
            return java.util.Objects.equals(ancestorId, key.ancestorId)
                    && java.util.Objects.equals(descendantId, key.descendantId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(ancestorId, descendantId);
        }
    }
}
