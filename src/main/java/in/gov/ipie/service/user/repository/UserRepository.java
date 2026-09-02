package in.gov.ipie.service.user.repository;

import java.util.Optional;
import java.util.UUID;

import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.VisibilityScope;

/**
 * Domain-owned port for User persistence. The application layer depends only on this interface -
 * the JPA-backed implementation lives in the infrastructure layer (master standards doc, 5.1:
 * "Controllers should not call repositories directly" / section 16 layering rules).
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** Same email check as {@link #existsByEmailIgnoreCase(String)} but excluding one user, for update flows. */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID excludedId);

    boolean existsByPhoneNumber(String phoneNumber);

    /** Looks up the in-flight verification token from the pillar-admin email link. */
    Optional<User> findByVerificationTokenHash(String verificationTokenHash);

    /** Resolves the current caller (the JWT {@code sub} claim) for {@code GET /api/v1/users/me}. */
    Optional<User> findByKeycloakUserId(UUID keycloakUserId);

    PageResult<User> search(UserSearchCriteria criteria, VisibilityScope visibleTo, PageRequest pageRequest);

    /**
     * Keyset ("seek") variant of {@link #search} - ordered by {@code (createdAt, id)} ascending,
     * no {@code COUNT(*)}. Prefer this over {@link #search} for large/high-traffic listings; see
     * {@code CursorPageRequest}'s Javadoc.
     */
    CursorPageResult<User> searchAfter(UserSearchCriteria criteria, VisibilityScope visibleTo, CursorPageRequest pageRequest);
}

