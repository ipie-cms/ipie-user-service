package in.gov.ipie.service.user.persistence;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.VisibilityScope;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Public so the {@code repository} subpackage - the only other caller - can use it across the
 * package boundary.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserJpaEntity> matching(UserSearchCriteria criteria) {
        // Entity principals live in `users` too since V13 - an organisation IS a principal - but a
        // user search is a search for people. Without this, every admin listing would start
        // returning rows named `cin-u74140dl2015ptc123456` with an @entity.invalid address, which
        // reads as corrupt data rather than as a deliberate model.
        Specification<UserJpaEntity> specification = (root, query, cb) -> cb.isFalse(root.get("isOrg"));

        if (criteria.usernameContains() != null && !criteria.usernameContains().isBlank()) {
            String pattern = "%" + criteria.usernameContains().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("username")), pattern));
        }
        if (criteria.emailContains() != null && !criteria.emailContains().isBlank()) {
            String pattern = "%" + criteria.emailContains().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), pattern));
        }
        if (criteria.status() != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), criteria.status()));
        }

        return specification;
    }

    /**
     * Narrows a search to what one caller may see, as a predicate on the query itself.
     *
     * <p>It has to be a {@code WHERE} clause and not a filter over the results: a fetch-then-filter
     * returns short pages, reports totals that count rows the caller may not see, and makes the size
     * of a hidden population inferable from the numbers (10.4). Applied here, the database never
     * hands back a row the caller is not entitled to.
     *
     * <p>The axes are OR-ed. A caller sees their own record, plus anything in the organisation
     * subtree they administer, plus anything their pillar validated. AND-ing them would return
     * nothing for the commonest case, since an insolvency professional carries a pillar and no
     * organisation - exactly the population an IBBI administrator exists to see.
     */
    public static Specification<UserJpaEntity> visibleTo(VisibilityScope scope) {
        if (scope.unrestricted()) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            // Always true, never empty: a caller with no administrative reach still sees themselves,
            // so this can never degenerate into a predicate that matches nothing.
            Predicate visible = cb.equal(root.get("id"), scope.selfUserId());
            if (!scope.hierarchyRootIds().isEmpty()) {
                // The descendants stay in the database. Materialising them into an IN list would
                // make the query text grow with the caller's own org chart; as a subquery the
                // planner uses organisation_closure's index and the application never sees the ids.
                Subquery<UUID> descendants = query.subquery(UUID.class);
                Root<OrganisationClosureJpaEntity> closure = descendants.from(OrganisationClosureJpaEntity.class);
                descendants.select(closure.get("descendantId"))
                        .where(closure.get("ancestorId").in(scope.hierarchyRootIds()));
                visible = cb.or(visible, root.get("organisationId").in(descendants));
            }
            if (!scope.pillarScopes().isEmpty()) {
                visible = cb.or(visible, root.get("pillarScope").in(scope.pillarScopes()));
            }
            return visible;
        };
    }
}
