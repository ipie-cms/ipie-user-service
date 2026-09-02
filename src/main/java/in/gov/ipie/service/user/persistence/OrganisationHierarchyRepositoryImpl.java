package in.gov.ipie.service.user.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import in.gov.ipie.service.user.repository.OrganisationHierarchyRepository;
import jakarta.persistence.EntityManager;

@Repository
public class OrganisationHierarchyRepositoryImpl implements OrganisationHierarchyRepository {

    /**
     * A recursive CTE, and the {@code UNION} is doing real work: it deduplicates, which is what stops
     * a cycle in {@code parent_id} from looping forever. The single-row CHECK in V9 prevents a node
     * being its own parent and nothing prevents a longer cycle, so the query must survive one rather
     * than assume it cannot happen.
     *
     * <p>Soft-deleted nodes are excluded: a deactivated organisation should not carry its users into
     * an administrator's view, and its children are unreachable through it by the same reasoning.
     */
    /**
     * Reads the closure table rather than walking the tree. V12 maintains it on every change to
     * {@code parent_id}, so the transitive relation is already stored and this is one indexed lookup
     * instead of a recursion whose cost grows with depth and is paid on every request.
     *
     * <p>Only the Elasticsearch path needs the ids in hand; the Postgres path expresses the same
     * relation as a subquery and never calls this.
     */
    private static final String SUBTREE = """
            SELECT descendant_id FROM organisation_closure WHERE ancestor_id = :rootId
            """;

    private final EntityManager entityManager;

    public OrganisationHierarchyRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> subtreeOf(UUID rootId) {
        if (rootId == null) {
            return Set.of();
        }
        @SuppressWarnings("unchecked")
        List<UUID> ids = entityManager.createNativeQuery(SUBTREE, UUID.class)
                .setParameter("rootId", rootId)
                .getResultList();
        return new HashSet<>(ids);
    }
}
