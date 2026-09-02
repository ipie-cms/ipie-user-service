-- `users` becomes a principal table, and the detail moves out of it.
--
-- Settled by the programme on 2026-08-17 (working plan 10.2). `users` holds 37 columns because it
-- holds four unrelated things at once: the principal, the person, the postal address and the
-- transient registration workflow. This is the first of those cuts and the one the rest depend on -
-- the OTP and verification columns can move to a registration table afterwards without disturbing
-- identity, and the address can become a shared type instead of being spelled two different ways in
-- `users` and `organisations`.
--
-- EVERY PRINCIPAL IS A USER ROW. A person is one; an entity is one. "Each entity creates a user
-- irrespective of the authorised representative of that entity, so two users get created for an
-- entity" - the entity's own principal, and the person authorised to act for it. That is what makes
-- "an IPE is considered an IP" expressible without a second parallel vocabulary, and it is why
-- `organisation_professional_roles` (V11) is folded back below: with the entity holding a user row,
-- its qualification belongs in `user_professional_roles` alongside every other principal's.
--
-- THE REFERENCE POINTS FROM THE DETAIL TO THE PRINCIPAL. `person.user_id` and
-- `organisations.user_id`, each UNIQUE NOT NULL. The earlier sketch had `users.ref_id` pointing the
-- other way with `is_org` saying which table it meant; that direction admits no foreign key at all,
-- because one column cannot reference two tables. Reversed, the database enforces both that every
-- detail row has a principal and that no two rows claim the same one.
--
-- `is_org` IS A CACHED COPY OF A DERIVABLE FACT - which detail table holds the row. It earns its
-- place because "is this principal an entity" is asked on every authorisation path and should not
-- cost a join, but it is denormalised and can therefore disagree with reality. It must be written in
-- the same transaction as the detail row, which is a cross-table invariant no plain constraint can
-- express - so this migration checks it at the end rather than assuming it. A principal with
-- `is_org` false and no `person` row is invisible to every screen that reads a name, which reads as
-- missing data rather than as a broken invariant.
--
-- WHAT AN ENTITY PRINCIPAL IS CALLED. `username` and `email` are UNIQUE NOT NULL, and an
-- organisation has neither. Both are derived from the government id the row already carries -
-- `id_type`/`id_value` are NOT NULL and unique together, so `pan-aabcu9603r` is unique whenever the
-- entity is identified by its PAN, and `cin-...`/`llpin-...`/`tan-...` otherwise. The address uses
-- the reserved `.invalid` domain deliberately: it can never be delivered to, so nothing can quietly
-- start mailing an entity principal in place of the person authorised to act for it. The entity's
-- real contact address stays on `organisations.contact_email`, where it already lives.
--
-- IDENTITY PROOF MOVES AS IT STANDS, one type plus a hash and a masked last-4. The catalogue holds
-- exactly PAN and AADHAAR (V2), so the column pair already expresses "PAN, or Aadhaar where there is
-- no PAN" - the rule the programme stated on 2026-08-18 - with the type saying which was taken. It
-- is deliberately NOT enforced here as a check constraint: a PRE_REGISTRATION draft legitimately has
-- neither yet, so a NOT NULL-shaped rule would fail on migrate and would be wrong about the domain
-- besides. The rule belongs at registration, and collecting Aadhaar there carries DPDP obligations -
-- purpose limitation and an itemised consent record, for which `consent_notices`/`user_consents`
-- already exist - which a column constraint cannot express either.
--
-- NOT IN THIS CHANGE, deliberately: `org_partners`, `user_hierarchy` and `ip_afa`, and the
-- retirement of `users.organisation_id`/`organisations.parent_id`. The last of those would
-- invalidate the closure table V12 built, and collides with the still-open FRS item 9 requirement
-- for `parent_id` plus COUNTRY/REGIONAL/ZONAL levels. One cut at a time.

-- ============================================================================================
-- 1. The discriminator
-- ============================================================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_org BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN users.is_org IS
    'True when this principal is an entity and its detail is in organisations; false when it is a '
    'person and its detail is in person. A cached copy of which detail table holds the row - kept '
    'because every authorisation path asks it, and written in the same transaction as the detail.';

-- ============================================================================================
-- 2. person - the detail a principal has because it is a person
-- ============================================================================================
-- ids default to uuidv7() rather than gen_random_uuid(): Postgres 18 (the version this stack and
-- the Testcontainers fixture both run) generates version-7 natively, so rows inserted by a
-- migration are time-ordered exactly like the ones Hibernate's UuidV7Generator produces. A v4 id
-- here would scatter inserts across the index for no reason.

CREATE TABLE IF NOT EXISTS person (
    id                                  UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id                             UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Nullable for the same reason they were nullable on users: unknown until step 2 of
    -- self-registration fills the wizard in.
    full_name                           VARCHAR(200),
    phone_number                        VARCHAR(20),
    category                            VARCHAR(20),
    address_line1                       VARCHAR(500),
    address_line2                       VARCHAR(500),
    country                             VARCHAR(100),
    state                               VARCHAR(100),
    city                                VARCHAR(100),
    pin                                 VARCHAR(10),
    identity_proof_type_id              UUID REFERENCES identity_proof_types (id),
    identity_proof_number_hash          VARCHAR(64),
    identity_proof_number_last4         VARCHAR(4),
    -- The standard columns every table carries (master standards doc, Database Development
    -- Standards): audit stamps, the optimistic-lock version, and soft-delete state.
    created_at                          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                          VARCHAR(100) NOT NULL,
    updated_at                          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by                          VARCHAR(100) NOT NULL,
    version                             BIGINT NOT NULL DEFAULT 0,
    is_active                           BOOLEAN NOT NULL DEFAULT true,
    deleted_at                          TIMESTAMPTZ,
    deleted_by                          VARCHAR(100),
    CONSTRAINT uq_person_user_id UNIQUE (user_id),
    CONSTRAINT chk_person_category CHECK (
        category IS NULL OR category IN ('INDIAN', 'NRI', 'FOREIGNER'))
);

COMMENT ON TABLE person IS
    'Person detail for a principal: name, contact, postal address and identity proof. One row per '
    'users row with is_org = false, enforced by uq_person_user_id plus the check at the end of V13.';

-- ============================================================================================
-- 3. Move the rows before moving the columns
-- ============================================================================================
-- Every existing principal is a person - entity principals do not exist until section 5 below - so
-- this is one row per user, carrying that user's audit stamps rather than inventing new ones. The
-- NOT EXISTS guard makes a re-run a no-op, matching the style V9/V11 already use.

INSERT INTO person (user_id, full_name, phone_number, category,
                    address_line1, address_line2, country, state, city, pin,
                    identity_proof_type_id, identity_proof_number_hash, identity_proof_number_last4,
                    created_at, created_by, updated_at, updated_by, is_active, deleted_at, deleted_by)
SELECT u.id, u.full_name, u.phone_number, u.category,
       u.address_line1, u.address_line2, u.country, u.state, u.city, u.pin,
       u.identity_proof_type_id, u.identity_proof_number_hash, u.identity_proof_number_last4,
       u.created_at, u.created_by, u.updated_at, u.updated_by, u.is_active, u.deleted_at, u.deleted_by
  FROM users u
 WHERE NOT EXISTS (SELECT 1 FROM person p WHERE p.user_id = u.id);

-- ============================================================================================
-- 4. The indexes follow their columns
-- ============================================================================================
-- uq_users_phone_number and idx_users_full_name_trgm (V12) index columns that no longer live on
-- users. Recreated against person under names that say where they are, so that a reader of \d does
-- not have to remember a rename. Both keep their original shape - partial unique on a nullable
-- column, and a trigram GIN for the leading-wildcard search V12 exists for.

DROP INDEX IF EXISTS uq_users_phone_number;
CREATE UNIQUE INDEX IF NOT EXISTS uq_person_phone_number
    ON person (phone_number) WHERE phone_number IS NOT NULL;

DROP INDEX IF EXISTS idx_users_full_name_trgm;
CREATE INDEX IF NOT EXISTS idx_person_full_name_trgm
    ON person USING gin (lower(full_name) gin_trgm_ops);

-- ============================================================================================
-- 5. Every organisation gets its own principal
-- ============================================================================================
-- The id is assigned to the organisation first and the principal inserted with it, rather than
-- inserting and correlating afterwards: an INSERT ... RETURNING cannot hand back the organisation
-- row it came from, and matching on the derived username afterwards would make the join depend on
-- the derivation being collision-free, which is exactly the property being relied on elsewhere.

ALTER TABLE organisations
    ADD COLUMN IF NOT EXISTS user_id UUID;

UPDATE organisations SET user_id = uuidv7() WHERE user_id IS NULL;

INSERT INTO users (id, username, email, status, registration_status, is_org, notification_channels,
                   created_at, created_by, updated_at, updated_by, version, is_active)
SELECT o.user_id,
       lower(o.id_type) || '-' || lower(o.id_value),
       lower(o.id_type) || '-' || lower(o.id_value) || '@entity.invalid',
       'ACTIVE',
       -- Not PRE_REGISTRATION: no registration workflow was ever started for an entity principal
       -- and none will be. It is the authorised representative who registers, and their own row
       -- carries that lifecycle. Leaving these rows in a registration state would put entities into
       -- every pending-registration queue that filters on it.
       'VERIFIED',
       true,
       'EMAIL',
       o.created_at, o.created_by, now(), 'V13__person_and_entity_principals', 0, o.is_active
  FROM organisations o
 WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.id = o.user_id);

ALTER TABLE organisations ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE organisations DROP CONSTRAINT IF EXISTS uq_organisations_user_id;
ALTER TABLE organisations ADD CONSTRAINT uq_organisations_user_id UNIQUE (user_id);

ALTER TABLE organisations DROP CONSTRAINT IF EXISTS fk_organisations_user_id;
ALTER TABLE organisations ADD CONSTRAINT fk_organisations_user_id
    FOREIGN KEY (user_id) REFERENCES users (id);

COMMENT ON COLUMN organisations.user_id IS
    'The principal this entity is. UNIQUE NOT NULL, so every entity has exactly one users row and no '
    'two entities share one. Its username and email are derived from id_type/id_value; the address '
    'uses the reserved .invalid domain because an entity principal is not a mailbox.';

-- ============================================================================================
-- 6. One qualification table for both kinds of principal
-- ============================================================================================
-- organisation_professional_roles (V11) exists only so an organisation could hold the IP
-- qualification while `user_professional_roles` had a user_id and no counterpart. With the entity
-- holding a user row that reason is gone, and keeping both would leave two ways to say the same
-- thing. The value column is widened first rather than truncated into: V11 declared VARCHAR(100)
-- against V5's VARCHAR(50), and left(value, 50) would silently drop the tail of a longer
-- recognition number.

ALTER TABLE user_professional_roles
    ALTER COLUMN professional_identification_value TYPE VARCHAR(100);

INSERT INTO user_professional_roles (id, user_id, professional_role_id,
                                     professional_identification_type_id,
                                     professional_identification_value,
                                     created_at, created_by, updated_at, updated_by,
                                     version, is_active, deleted_at, deleted_by)
SELECT uuidv7(), o.user_id, opr.professional_role_id,
       opr.professional_identification_type_id, opr.professional_identification_value,
       opr.created_at, opr.created_by, opr.updated_at, opr.updated_by,
       opr.version, opr.is_active, opr.deleted_at, opr.deleted_by
  FROM organisation_professional_roles opr
  JOIN organisations o ON o.id = opr.organisation_id
 WHERE NOT EXISTS (
        SELECT 1 FROM user_professional_roles upr
         WHERE upr.user_id = o.user_id
           AND upr.professional_role_id = opr.professional_role_id);

DROP TABLE IF EXISTS organisation_professional_roles;

-- ============================================================================================
-- 7. The columns that moved come off users
-- ============================================================================================
-- Done last, so that everything above reads from them. chk_users_category goes with its column.

ALTER TABLE users
    DROP COLUMN IF EXISTS full_name,
    DROP COLUMN IF EXISTS phone_number,
    DROP COLUMN IF EXISTS category,
    DROP COLUMN IF EXISTS address_line1,
    DROP COLUMN IF EXISTS address_line2,
    DROP COLUMN IF EXISTS country,
    DROP COLUMN IF EXISTS state,
    DROP COLUMN IF EXISTS city,
    DROP COLUMN IF EXISTS pin,
    DROP COLUMN IF EXISTS identity_proof_type_id,
    DROP COLUMN IF EXISTS identity_proof_number_hash,
    DROP COLUMN IF EXISTS identity_proof_number_last4;

-- ============================================================================================
-- 8. The invariant, checked rather than assumed
-- ============================================================================================
-- No constraint can express "is_org agrees with which detail table holds the row", so it is
-- verified here against the data this migration just produced. Failing loudly beats leaving a
-- principal that every screen reading a name renders as blank.

DO $$
DECLARE
    people_without_detail   INT;
    entities_without_detail INT;
    people_marked_as_org    INT;
BEGIN
    SELECT count(*) INTO people_without_detail
      FROM users u
     WHERE NOT u.is_org
       AND NOT EXISTS (SELECT 1 FROM person p WHERE p.user_id = u.id);

    SELECT count(*) INTO entities_without_detail
      FROM users u
     WHERE u.is_org
       AND NOT EXISTS (SELECT 1 FROM organisations o WHERE o.user_id = u.id);

    SELECT count(*) INTO people_marked_as_org
      FROM person p JOIN users u ON u.id = p.user_id
     WHERE u.is_org;

    IF people_without_detail > 0 OR entities_without_detail > 0 OR people_marked_as_org > 0 THEN
        RAISE EXCEPTION
            'is_org invariant violated: % person principals with no person row, % entity principals with no organisation, % person rows against an entity principal',
            people_without_detail, entities_without_detail, people_marked_as_org;
    END IF;
END $$;

-- ============================================================================================
-- 9. Grants for the new table - and two that were missed
-- ============================================================================================
-- V4's list is explicit on purpose (this database is shared with ipie-iam-service, so "all tables
-- in schema" would hand over the credential tables). The cost of that choice is that a table added
-- later must grant itself, and V11 and V12 did not: `organisation_closure` is read by
-- UserSpecifications on every user search with a hierarchy scope, so in any environment where the
-- least-privilege roles exist that search fails with a permission error. It works locally only
-- because migrations there run as a superuser who owns everything. Granted here alongside `person`.
-- organisation_professional_roles needs nothing - section 6 dropped it.

DO $$
DECLARE
    new_tables text[] := ARRAY['person', 'organisation_closure'];
    target text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ipie_user_service_app') THEN
        RAISE NOTICE 'ipie_user_service_app does not exist - skipping grants (local development)';
        RETURN;
    END IF;

    FOREACH target IN ARRAY new_tables LOOP
        IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = target) THEN
            EXECUTE format(
                'GRANT SELECT, INSERT, UPDATE, DELETE ON public.%I TO ipie_user_service_app', target);
        ELSE
            RAISE WARNING 'V13: table % is listed but does not exist - grant skipped', target;
        END IF;
    END LOOP;
END $$;
