package in.gov.ipie.service.user.domain;

/**
 * The columns {@code GET /api/v1/users} may sort by - deliberately an allow-list, not an
 * arbitrary string. Each value names an actually-indexed column ({@code V2}/{@code V3}/{@code
 * V7} migrations), so accepting only these keeps every sort request backed by an index instead
 * of letting a caller request a sort on an unindexed (or non-existent) column. Binding this
 * directly as a {@code @RequestParam} type on {@code UserController} means an invalid value is
 * rejected by Spring's own enum conversion before it ever reaches JPA/Elasticsearch.
 */
public enum UserSortField {
    USERNAME("username"),
    EMAIL("email"),
    STATUS("status"),
    CREATED_AT("createdAt");

    private final String propertyName;

    UserSortField(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * The actual property/field name to sort by - identical spelling on both the JPA entity
     * ({@code UserJpaEntity}) and the Elasticsearch document ({@code UserDocument}), so one
     * mapping serves both {@code UserRepositoryImpl} and {@code ElasticsearchUserSearchIndex}.
     */
    public String propertyName() {
        return propertyName;
    }
}

