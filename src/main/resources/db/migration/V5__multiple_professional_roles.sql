-- A person may hold several professional roles at once, each with its own credential.
--
-- Reverts the single-role collapse. Two sources require multiplicity and neither is ambiguous:
--
--   FRS 1.1.1, item 6:  "Add Professional Role (can select multiple roles): (Admin, IP, RV, Legal
--                        Rep (Advocate/CA/CS, etc.), Authorized Rep)", with a Professional
--                        Identification Type and Value *for each role selected*.
--   IBBI, 13 Aug 2026 minutes, 3.5: "A single user account should support all roles performed by an
--                        Insolvency Professional, including IRP, RP, Liquidator and Authorised
--                        Representative (AR)."
--
-- The columns being replaced could hold exactly one role and one credential, so the second role a
-- person holds had nowhere to go - and the identification value is per role, not per person: an IP
-- carries an IBBI registration number while the same individual acting as a legal representative
-- carries a bar registration number. One column cannot hold both, and choosing between them loses
-- the ability to prove either.
--
-- WHY A CHILD TABLE RATHER THAN REPEATED COLUMNS. Roles are open-ended - the catalogue is seeded
-- data and grows - so any fixed number of column groups is a guess that a migration will have to
-- correct later. The unique constraint below is the rule that matters: the same role twice for one
-- person is meaningless, and a duplicate would double-count in any report grouping by role.
--
-- LEGAL REPRESENTATIVE TYPE MOVES TOO. Advocate/CA/CS qualifies the LEGAL_REPRESENTATIVE role
-- specifically, so it belongs to the row that says the person holds that role, not to the person. A
-- user who is both an IP and an advocate has one row carrying the type and one that does not.

CREATE TABLE user_professional_roles (
    id                                 UUID PRIMARY KEY,
    user_id                            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    professional_role_id               UUID NOT NULL REFERENCES professional_roles (id),
    professional_identification_type_id UUID REFERENCES professional_identification_types (id),
    professional_identification_value   VARCHAR(50),
    legal_representative_type_id        UUID REFERENCES legal_representative_types (id),
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
    CONSTRAINT uq_user_professional_roles UNIQUE (user_id, professional_role_id)
);

COMMENT ON TABLE user_professional_roles IS
    'One row per professional role a person holds, each with the credential proving that role. A '
    'person may hold several; the same role twice is refused.';
COMMENT ON COLUMN user_professional_roles.professional_identification_value IS
    'The registration number for this role - IBBI for IP and RV, bar or institute registration for a '
    'legal representative. Validated with the issuing body per role, not per person.';
COMMENT ON COLUMN user_professional_roles.legal_representative_type_id IS
    'Advocate, CA, CS - qualifies the LEGAL_REPRESENTATIVE role only, and is null on every other row.';

-- Lists "everyone holding role X" and "this person's roles" are both common; the unique constraint
-- already indexes (user_id, professional_role_id), so only the reverse direction needs one.
CREATE INDEX idx_user_professional_roles_role ON user_professional_roles (professional_role_id);

-- Carry across what the single columns held. Rows without a role had nothing to carry.
INSERT INTO user_professional_roles (
    id, user_id, professional_role_id, professional_identification_type_id,
    professional_identification_value, legal_representative_type_id,
    created_at, created_by, updated_at, updated_by)
SELECT gen_random_uuid(), u.id, u.professional_role_id, u.professional_identification_type_id,
       u.professional_identification_value, u.legal_representative_type_id,
       u.created_at, u.created_by, u.updated_at, u.updated_by
  FROM users u
 WHERE u.professional_role_id IS NOT NULL;

ALTER TABLE users
    DROP COLUMN professional_role_id,
    DROP COLUMN professional_identification_type_id,
    DROP COLUMN professional_identification_value,
    DROP COLUMN legal_representative_type_id;

-- The runtime role reaches its own tables by explicit grant, never by a blanket one (see V4).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ipie_user_service_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_professional_roles
            TO ipie_user_service_app;
    ELSE
        RAISE NOTICE 'ipie_user_service_app does not exist - skipping grant (local development)';
    END IF;
END
$$;
