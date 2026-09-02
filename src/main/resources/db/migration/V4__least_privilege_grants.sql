-- The runtime role gets exactly what it needs, and nothing else.
--
-- The roles themselves are created by the deployment, not here:
-- ipie-platform-mca/deploy/postgres/roles/01-create-roles.sql. This is guarded on their existence
-- and is a no-op without them, so a developer running against `postgres` sees no change.
--
-- WHY AN EXPLICIT LIST, RATHER THAN "ALL TABLES IN SCHEMA"
--
-- This service shares the `ipie_user_service` database with ipie-iam-service, so "all tables in this
-- schema" would include `user_credentials` and `credential_setup_tokens` - the two tables this
-- service must never read. A blanket grant would hand over precisely what the boundary exists to
-- withhold, and it would do it silently. "Tables owned by current_user" has the same flaw whenever
-- migrations run as `postgres`, which owns everything.
--
-- So the list is written out. It is a boundary decision and should read like one: adding a table
-- here is a deliberate act, reviewable in a diff. It runs last so that every table it names already
-- exists and each grant is explicit, rather than left to default privileges that only apply when
-- Flyway happens to run as the owner role.

DO $$
DECLARE
    owned_tables text[] := ARRAY[
        'users',
        'organisations',
        'stakeholder_links',
        'stakeholder_link_requests',
        'identity_proof_types',
        'legal_representative_types',
        'professional_identification_types',
        'professional_roles',
        'consent_notices',
        'user_consents',
        'audit_trail',
        'outbox_events',
        'processed_events'
        -- flyway_schema_history is deliberately absent: the app role never reads it, and only the
        -- owner role that runs migrations has any business writing it.
    ];
    target text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ipie_user_service_app') THEN
        RAISE NOTICE 'ipie_user_service_app does not exist - skipping grants (local development)';
        RETURN;
    END IF;

    GRANT USAGE ON SCHEMA public TO ipie_user_service_app;

    FOREACH target IN ARRAY owned_tables LOOP
        IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = target) THEN
            EXECUTE format(
                'GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO ipie_user_service_app', target);
        ELSE
            RAISE WARNING 'V30: table % is listed but does not exist - grant skipped', target;
        END IF;
    END LOOP;

    -- No TRUNCATE, no REFERENCES, no TRIGGER: the running application needs none of them, and
    -- TRUNCATE in particular would let it erase an audit table it is only supposed to append to.

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ipie_user_service_owner') THEN
        ALTER DEFAULT PRIVILEGES FOR ROLE ipie_user_service_owner IN SCHEMA public
            GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO ipie_user_service_app;
    END IF;
END
$$;
