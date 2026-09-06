package in.gov.ipie.service.user.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import in.gov.ipie.common.persistence.IntegrityViolations;
import in.gov.ipie.common.core.paging.Cursor;
import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.exception.UserNotFoundException;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.VisibilityScope;
import in.gov.ipie.service.user.repository.UserRepository;

/**
 * Infrastructure-layer adapter implementing the domain-owned {@link UserRepository} port on top
 * of Spring Data JPA. The only class in this service allowed to know about {@link UserJpaEntity}
 * and {@link UserJpaRepository}.
 */
@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class UserRepositoryImpl implements UserRepository {

    /**
     * The constraints on {@code users}, each named so a caller learns which field they actually
     * repeated. This replaced a single unconditional message that answered "username or email" for
     * every violation - including a duplicate phone number, which it was simply wrong about.
     */
    static final IntegrityViolations VIOLATIONS = IntegrityViolations.forTable()
            .primaryKey("users_pkey")
            .conflict("uq_users_username", "A user with this username already exists")
            .conflict("uq_users_email", "A user with this email address already exists")
            .conflict("uq_users_verification_token_hash", "That verification token is already in use")
            .build();

    private final UserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        try {
            if (user.getId() == null) {
                UserJpaEntity saved = jpaRepository.save(mapper.toNewEntity(user));
                return mapper.toDomain(saved);
            }

            UserJpaEntity entity = jpaRepository.findById(user.getId())
                    .orElseThrow(() -> new UserNotFoundException(user.getId()));
            mapper.copyMutableFieldsOnto(user, entity);
            return mapper.toDomain(jpaRepository.save(entity));
        } catch (DataIntegrityViolationException e) {
            throw VIOLATIONS.translate(e);
        }
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return jpaRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID excludedId) {
        return jpaRepository.existsByEmailIgnoreCaseAndIdNot(email, excludedId);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return jpaRepository.existsByPersonPhoneNumber(phoneNumber);
    }

    @Override
    public Optional<User> findByVerificationTokenHash(String verificationTokenHash) {
        return jpaRepository.findByVerificationTokenHash(verificationTokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByKeycloakUserId(UUID keycloakUserId) {
        return jpaRepository.findByKeycloakUserId(keycloakUserId).map(mapper::toDomain);
    }

    @Override
    public PageResult<User> search(UserSearchCriteria criteria, VisibilityScope visibleTo, PageRequest pageRequest) {
        Pageable pageable = toPageable(pageRequest);
        Page<UserJpaEntity> page = jpaRepository.findAll(
                UserSpecifications.matching(criteria).and(UserSpecifications.visibleTo(visibleTo)), pageable);
        List<User> content = page.getContent().stream().map(mapper::toDomain).toList();
        return PageResult.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public CursorPageResult<User> searchAfter(UserSearchCriteria criteria, VisibilityScope visibleTo, CursorPageRequest pageRequest) {
        Optional<Cursor> cursor = pageRequest.decodeCursor();
        Instant afterCreatedAt = cursor.map(Cursor::createdAt).orElse(null);
        UUID afterId = cursor.map(Cursor::id).orElse(null);

        List<UserJpaEntity> rows = jpaRepository.searchAfter(
                UserSpecifications.matching(criteria).and(UserSpecifications.visibleTo(visibleTo)),
                afterCreatedAt, afterId, pageRequest.size() + 1);

        boolean hasMore = rows.size() > pageRequest.size();
        List<UserJpaEntity> page = hasMore ? rows.subList(0, pageRequest.size()) : rows;
        List<User> content = page.stream().map(mapper::toDomain).toList();

        String nextCursor = hasMore
                ? new Cursor(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId()).encode()
                : null;

        return CursorPageResult.of(content, nextCursor, hasMore);
    }

    private static Pageable toPageable(PageRequest pageRequest) {
        if (pageRequest.sortBy() == null || pageRequest.sortBy().isBlank()) {
            return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        }
        Sort.Direction direction = pageRequest.sortDirection() == PageRequest.SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.page(), pageRequest.size(), Sort.by(direction, pageRequest.sortBy()));
    }
}

