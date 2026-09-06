-- Two changes that only matter at volume, both cheap now and expensive once the tables are large.
--
-- ============================================================================================
-- 1. SEARCH: a leading-wildcard LIKE cannot use a btree.
-- ============================================================================================
-- UserSpecifications.matching builds `lower(username) LIKE '%pattern%'`, and idx_users_email_lower
-- serves prefix and equality only - a pattern that starts with a wildcard forces a sequential scan
-- of every user. At the tens of thousands seeded here that is invisible; at the millions this
-- platform is sized for (a creditor per case) it is a full table read per keystroke of an admin
-- search box.
--
-- A trigram GIN index is the standard answer and needs no query change: Postgres uses it for
-- LIKE/ILIKE with wildcards on either side. pg_trgm is a contrib extension, present in the official
-- image and in RDS/Azure/Cloud SQL alike.
--
-- Elasticsearch remains the better home for real search and ipie-user-service already has a second
-- backend for it - but it is NOT configured in the running stack, so the JPA path is what actually
-- serves today. Indexing it is not a reason to skip ES; it is a refusal to let the default
-- deployment be the one that cannot scale.

-- CREATE EXTENSION here as well as in deploy/postgres/roles/01-create-roles.sql, and the duplication
-- is deliberate. Creating an extension needs CREATE on the *database*, which the owner role does not
-- have - it owns tables, not the database - so the bootstrap script installs it as a superuser once
-- per environment. But IF NOT EXISTS short-circuits when the extension is already present and needs
-- no privilege to do so (verified: the owner role gets a notice and success), so this line is a
-- no-op wherever the bootstrap has run, and creates it wherever the migration user may - a
-- Testcontainers database in the integration tests, or a managed instance where pg_trgm is trusted.
-- An environment with neither fails here with a privilege error naming the extension, which is the
-- honest outcome: the alternative is skipping the index quietly and discovering the sequential scan
-- in production.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_users_username_trgm ON users USING gin (lower(username) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_email_trgm    ON users USING gin (lower(email) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_full_name_trgm ON users USING gin (lower(full_name) gin_trgm_ops);

-- ============================================================================================
-- 2. HIERARCHY: a closure table, so visibility is a join rather than a graph walk per request.
-- ============================================================================================
-- VisibilityScope resolves an administrator's subtree with a recursive CTE and then filters users
-- with `organisation_id IN (<materialised ids>)`. Two problems arrive together at scale: the walk
-- runs on every list request, and the IN list is bounded by nothing - a large entity produces a
-- query whose text grows with its own org chart.
--
-- A closure table stores the transitive relation once (every ancestor-descendant pair, plus depth)
-- and moves the cost to writes, which are rare: an organisation's parent changes far less often than
-- its users are listed. Visibility then becomes a subquery the planner can use directly, with no ids
-- crossing the application boundary at all.
--
-- The trigger, rather than application code, maintains it. A closure table that disagrees with
-- parent_id silently returns the wrong rows to an administrator, which is a security answer and not
-- a caching one - so it is kept true by the same transaction that changes the tree, and cannot be
-- bypassed by a migration, a fixture or a second writer.
--
-- Depth is stored though nothing reads it yet: FRS item 9's "direct hierarchical structure" cascade
-- may need "children but not grandchildren", and adding the column later means rebuilding the table.

CREATE TABLE IF NOT EXISTS organisation_closure (
    ancestor_id   UUID NOT NULL REFERENCES organisations (id) ON DELETE CASCADE,
    descendant_id UUID NOT NULL REFERENCES organisations (id) ON DELETE CASCADE,
    depth         INTEGER NOT NULL,
    PRIMARY KEY (ancestor_id, descendant_id)
);

-- The lookup visibility performs: "every descendant of this node". Descendant-first covers the
-- reverse question ("who are my ancestors") without a second scan.
CREATE INDEX IF NOT EXISTS idx_organisation_closure_descendant ON organisation_closure (descendant_id, ancestor_id);

CREATE OR REPLACE FUNCTION rebuild_organisation_closure() RETURNS void AS $$
BEGIN
    DELETE FROM organisation_closure;
    -- UNION rather than UNION ALL: it deduplicates, which is what stops a cycle in parent_id from
    -- recursing forever. A single-row CHECK prevents a node being its own parent and nothing
    -- prevents a longer cycle, so this has to survive one rather than assume it cannot happen.
    INSERT INTO organisation_closure (ancestor_id, descendant_id, depth)
    WITH RECURSIVE walk(ancestor_id, descendant_id, depth) AS (
        SELECT id, id, 0 FROM organisations WHERE deleted_at IS NULL
        UNION
        SELECT w.ancestor_id, o.id, w.depth + 1
          FROM organisations o
          JOIN walk w ON o.parent_id = w.descendant_id
         WHERE o.deleted_at IS NULL
    )
    SELECT ancestor_id, descendant_id, depth FROM walk;
END;
$$ LANGUAGE plpgsql;

-- A full rebuild on every tree change, deliberately. Incremental maintenance of a closure table is
-- where the subtle bugs live - a reparent has to detach one subtree and reattach it against every
-- ancestor - and organisations number in the thousands, not the millions. The expensive table is
-- users, and this one is not it.
CREATE OR REPLACE FUNCTION organisation_closure_sync() RETURNS TRIGGER AS $$
BEGIN
    PERFORM rebuild_organisation_closure();
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_organisation_closure_sync ON organisations;
CREATE TRIGGER trg_organisation_closure_sync
    AFTER INSERT OR DELETE OR UPDATE OF parent_id, deleted_at ON organisations
    FOR EACH STATEMENT EXECUTE FUNCTION organisation_closure_sync();

SELECT rebuild_organisation_closure();
