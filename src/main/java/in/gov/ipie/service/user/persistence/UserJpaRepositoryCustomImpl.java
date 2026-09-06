package in.gov.ipie.service.user.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;


/**
 * {@code Impl}-suffixed so Spring Data JPA's repository factory auto-detects this as the fragment
 * backing {@link UserJpaRepositoryCustom} and wires it into {@link UserJpaRepository} - the
 * constructor's {@link EntityManager} parameter is injected automatically by that same factory,
 * no {@code @PersistenceContext} needed.
 */
class UserJpaRepositoryCustomImpl implements UserJpaRepositoryCustom {

    private final EntityManager entityManager;

    UserJpaRepositoryCustomImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<UserJpaEntity> searchAfter(Specification<UserJpaEntity> spec, Instant afterCreatedAt, UUID afterId, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserJpaEntity> query = cb.createQuery(UserJpaEntity.class);
        Root<UserJpaEntity> root = query.from(UserJpaEntity.class);

        Predicate predicate = combine(cb, spec.toPredicate(root, query, cb), keysetPredicate(cb, root, afterCreatedAt, afterId));
        if (predicate != null) {
            query.where(predicate);
        }
        query.orderBy(cb.asc(root.get("createdAt")), cb.asc(root.get("id")));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }

    private static Predicate keysetPredicate(CriteriaBuilder cb, Root<UserJpaEntity> root, Instant afterCreatedAt, UUID afterId) {
        if (afterCreatedAt == null || afterId == null) {
            return null;
        }
        return cb.or(
                cb.greaterThan(root.get("createdAt"), afterCreatedAt),
                cb.and(cb.equal(root.get("createdAt"), afterCreatedAt), cb.greaterThan(root.get("id"), afterId)));
    }

    private static Predicate combine(CriteriaBuilder cb, Predicate first, Predicate second) {
        if (first == null) {
            return second;
        }
        return second == null ? first : cb.and(first, second);
    }
}

