-- Renames the account-federation tables from "stakeholder" to "pillar", completing the vocabulary
-- split settled on 2026-08-17 (ARCHITECTURE_WORKING_PLAN.md 10.1).
--
-- These tables never held stakeholders. A row here links an iPIE user to their identity at IBBI,
-- NCLT, NCLAT, MCA or NeSL - the five **pillars**. A stakeholder is the other thing entirely: a user
-- of iPIE under the umbrella of a pillar, an IP/IPE or an entity, and the FRS gives that population
-- a microservice of its own. That service does not exist yet, so nothing yet contests the name -
-- which is exactly why the rename is cheap today and expensive later: the first time a genuine
-- stakeholder table appears, `stakeholder_links` would sit beside it meaning something unrelated.
--
-- RENAMING A TABLE IN POSTGRES DOES NOT RENAME ITS INDEXES OR CONSTRAINTS, so each is renamed
-- explicitly. Leaving them would work and read as a half-finished job in every \d output, which is
-- where the next person looks to learn what these tables are.
--
-- GRANTS SURVIVE UNTOUCHED. Privileges are held against the table's OID, not its name, so the
-- least-privilege grants from V4 follow the rename without being reissued. The scripts that grant
-- them (deploy/postgres/roles/01-create-roles.sql) are updated to the new names for readability
-- only.
--
-- IDEMPOTENT THROUGHOUT. A running environment is repaired by hand before this migration reaches
-- it, so every step checks the catalogue first and a second run is a no-op.

DO $$
BEGIN
    IF to_regclass('public.stakeholder_links') IS NOT NULL THEN
        ALTER TABLE stakeholder_links RENAME TO pillar_links;
    END IF;
    IF to_regclass('public.stakeholder_link_requests') IS NOT NULL THEN
        ALTER TABLE stakeholder_link_requests RENAME TO pillar_link_requests;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_name = 'pillar_links' AND column_name = 'stakeholder_type') THEN
        ALTER TABLE pillar_links RENAME COLUMN stakeholder_type TO pillar_type;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_name = 'pillar_links' AND column_name = 'external_stakeholder_id') THEN
        ALTER TABLE pillar_links RENAME COLUMN external_stakeholder_id TO external_pillar_id;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_name = 'pillar_link_requests' AND column_name = 'stakeholder_type') THEN
        ALTER TABLE pillar_link_requests RENAME COLUMN stakeholder_type TO pillar_type;
    END IF;
END $$;

-- Indexes and named check constraints, so \d reads consistently.
DO $$
DECLARE
    obj RECORD;
BEGIN
    FOR obj IN SELECT indexname AS n FROM pg_indexes
                WHERE schemaname = 'public' AND indexname LIKE '%stakeholder_link%'
    LOOP
        EXECUTE format('ALTER INDEX %I RENAME TO %I', obj.n, replace(obj.n, 'stakeholder_link', 'pillar_link'));
    END LOOP;

    FOR obj IN SELECT c.conname AS n, t.relname AS tbl
                 FROM pg_constraint c JOIN pg_class t ON t.oid = c.conrelid
                WHERE c.conname LIKE '%stakeholder%' AND t.relname IN ('pillar_links', 'pillar_link_requests')
    LOOP
        EXECUTE format('ALTER TABLE %I RENAME CONSTRAINT %I TO %I',
                       obj.tbl, obj.n, replace(replace(replace(obj.n, 'external_stakeholder_id', 'external_pillar_id'), 'stakeholder_link', 'pillar_link'), 'stakeholder_type', 'pillar_type'));
    END LOOP;
END $$;
