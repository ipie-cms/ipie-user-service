package in.gov.ipie.service.user.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;


/**
 * Manual keyset-pagination query, implemented directly against the {@code EntityManager} (see
 * {@code UserJpaRepositoryCustomImpl}) rather than {@code JpaSpecificationExecutor.findAll(Specification,
 * Pageable)} - that method always issues a {@code COUNT(*)} alongside the content query, which is
 * exactly the cost keyset pagination exists to avoid. Public so the sibling {@code repositoryimpl}
 * package can use it, by the same convention as {@link UserJpaRepository}.
 */
public interface UserJpaRepositoryCustom {

    /**
     * Rows matching {@code spec} ordered by {@code (createdAt, id)} ascending, starting strictly
     * after {@code afterCreatedAt}/{@code afterId} (both {@code null} for the first page), capped
     * at {@code limit} rows - callers request {@code pageSize + 1} to detect {@code hasMore}
     * without a separate count query.
     */
    List<UserJpaEntity> searchAfter(Specification<UserJpaEntity> spec, Instant afterCreatedAt, UUID afterId, int limit);
}

