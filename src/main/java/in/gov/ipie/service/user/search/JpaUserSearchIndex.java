package in.gov.ipie.service.user.search;

import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.VisibilityScope;
import in.gov.ipie.service.user.repository.UserRepository;
import in.gov.ipie.service.user.repository.UserSearchIndex;

/**
 * Fallback {@link UserSearchIndex}: delegates straight to the JPA-backed {@link UserRepository},
 * which is both the write model and (in this mode) the search read model. Registered by
 * {@code UserSearchIndexConfig} only when no Elasticsearch cluster is configured (e.g. a bare
 * {@code java -jar} run with no {@code spring.elasticsearch.uris}) - {@code UserService} only
 * depends on the {@link UserSearchIndex} port, so it never needs to know which implementation is
 * active. Public so the parent-package config class can construct it.
 */
public class JpaUserSearchIndex implements UserSearchIndex {

    private final UserRepository userRepository;

    public JpaUserSearchIndex(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PageResult<User> search(UserSearchCriteria criteria, VisibilityScope visibleTo, PageRequest pageRequest) {
        return userRepository.search(criteria, visibleTo, pageRequest);
    }

    @Override
    public CursorPageResult<User> searchAfter(UserSearchCriteria criteria, VisibilityScope visibleTo, CursorPageRequest pageRequest) {
        return userRepository.searchAfter(criteria, visibleTo, pageRequest);
    }

    @Override
    public void index(User user) {
        // No-op: Postgres is already both the source of truth and the search index in this mode.
    }
}

