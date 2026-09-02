package in.gov.ipie.service.user.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import in.gov.ipie.common.core.paging.Cursor;
import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.user.domain.User;
import in.gov.ipie.service.user.domain.UserSearchCriteria;
import in.gov.ipie.service.user.domain.VisibilityScope;
import in.gov.ipie.service.user.repository.UserSearchIndex;
import in.gov.ipie.service.user.repository.OrganisationHierarchyRepository;

/**
 * Real {@link UserSearchIndex} backed by Elasticsearch. Registered by
 * {@code UserSearchIndexConfig} only when {@code spring.elasticsearch.uris} is configured. Public
 * so that parent-package config class can construct it.
 *
 * <p>{@link #index(User)} forces an immediate index refresh so a just-written user is
 * searchable straight away - simpler and more deterministic (for a reference template and its
 * tests) than tuning around Elasticsearch's default ~1s near-real-time refresh interval, at the
 * cost of write throughput a higher-volume service would need to reconsider.
 */
public class ElasticsearchUserSearchIndex implements UserSearchIndex {

    private final ElasticsearchOperations operations;
    private final UserDocumentRepository documentRepository;
    private final UserSearchDocumentMapper mapper;
    private final OrganisationHierarchyRepository hierarchy;

    public ElasticsearchUserSearchIndex(
            ElasticsearchOperations operations, UserDocumentRepository documentRepository,
            UserSearchDocumentMapper mapper, OrganisationHierarchyRepository hierarchy) {
        this.operations = operations;
        this.documentRepository = documentRepository;
        this.mapper = mapper;
        this.hierarchy = hierarchy;
    }

    @Override
    public PageResult<User> search(UserSearchCriteria criteria, VisibilityScope visibleTo, PageRequest pageRequest) {
        Pageable pageable = toPageable(pageRequest);
        NativeQuery query = NativeQuery.builder()
                .withQuery(toQuery(criteria, visibleTo))
                .withPageable(pageable)
                .build();

        SearchHits<UserDocument> hits = operations.search(query, UserDocument.class);
        List<User> content = hits.stream().map(hit -> mapper.toDomain(hit.getContent())).toList();
        return PageResult.of(content, pageable.getPageNumber(), pageable.getPageSize(), hits.getTotalHits());
    }

    @Override
    public CursorPageResult<User> searchAfter(UserSearchCriteria criteria, VisibilityScope visibleTo, CursorPageRequest pageRequest) {
        Optional<Cursor> cursor = pageRequest.decodeCursor();
        int fetchSize = pageRequest.size() + 1;

        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(toQuery(criteria, visibleTo))
                .withSort(Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by(Sort.Direction.ASC, "idSort")))
                .withMaxResults(fetchSize);
        // The date-sort value must be the epoch-millisecond long Elasticsearch itself returns in a
        // hit's `sort` array for a date-typed field - not any other Instant representation - or
        // search_after silently resumes from the wrong position (see Elasticsearch's own
        // search_after documentation for this exact [date-as-millis, tiebreaker] pattern).
        cursor.ifPresent(c -> queryBuilder.withSearchAfter(List.of(c.createdAt().toEpochMilli(), c.id().toString())));

        SearchHits<UserDocument> hits = operations.search(queryBuilder.build(), UserDocument.class);
        List<UserDocument> rows = hits.stream().map(SearchHit::getContent).toList();

        boolean hasMore = rows.size() > pageRequest.size();
        List<UserDocument> page = hasMore ? rows.subList(0, pageRequest.size()) : rows;
        List<User> content = page.stream().map(mapper::toDomain).toList();

        String nextCursor = hasMore
                ? new Cursor(page.get(page.size() - 1).getCreatedAt(), UUID.fromString(page.get(page.size() - 1).getId())).encode()
                : null;

        return CursorPageResult.of(content, nextCursor, hasMore);
    }

    @Override
    public void index(User user) {
        documentRepository.save(mapper.toDocument(user));
        operations.indexOps(UserDocument.class).refresh();
    }

    private Query toQuery(UserSearchCriteria criteria, VisibilityScope visibleTo) {
        List<Query> filters = new ArrayList<>();

        if (criteria.usernameContains() != null && !criteria.usernameContains().isBlank()) {
            String pattern = "*" + criteria.usernameContains().toLowerCase(Locale.ROOT) + "*";
            filters.add(Query.of(q -> q.wildcard(w -> w.field("usernameLower").value(pattern))));
        }
        if (criteria.emailContains() != null && !criteria.emailContains().isBlank()) {
            String pattern = "*" + criteria.emailContains().toLowerCase(Locale.ROOT) + "*";
            filters.add(Query.of(q -> q.wildcard(w -> w.field("emailLower").value(pattern))));
        }
        if (criteria.status() != null) {
            filters.add(Query.of(q -> q.term(t -> t.field("status").value(criteria.status().name()))));
        }

        // The same predicate JpaUserSearchIndex applies as a WHERE clause, expressed for this
        // backend. It is a `filter` inside the outer bool, so it is mandatory and unscored: a
        // caller can never widen it through the criteria they send. Without it this implementation
        // would answer every search with every user while the Postgres one stayed correct - a
        // divergence nothing would report, since both return a well-formed page.
        if (!visibleTo.unrestricted()) {
            List<Query> allowed = new ArrayList<>();
            if (visibleTo.selfUserId() != null) {
                allowed.add(Query.of(q -> q.term(t -> t.field("id").value(visibleTo.selfUserId().toString()))));
            }
            for (UUID organisationId : hierarchy.expand(visibleTo.hierarchyRootIds())) {
                allowed.add(Query.of(q -> q.term(t -> t.field("organisationId").value(organisationId.toString()))));
            }
            for (String pillar : visibleTo.pillarScopes()) {
                allowed.add(Query.of(q -> q.term(t -> t.field("pillarScope").value(pillar))));
            }
            // No visible identity at all means no rows, expressed as an unsatisfiable clause rather
            // than by returning early - an empty `should` list would match everything.
            filters.add(Query.of(q -> q.bool(b -> b.should(allowed).minimumShouldMatch("1"))));
        }

        if (filters.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        return Query.of(q -> q.bool(b -> {
            filters.forEach(b::must);
            return b;
        }));
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

