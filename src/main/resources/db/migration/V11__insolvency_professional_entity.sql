-- An IPE is itself an insolvency professional, and until now nothing could say so.
--
-- The catch the programme raised on 2026-08-17: more than one IP together form an Insolvency
-- Professional Entity, and the IPE *is considered an IP*. The model treated the qualification as
-- something only a person can hold - `user_professional_roles` has a `user_id` and no counterpart -
-- and treated an organisation as a container for people rather than as a principal in its own right.
-- Both are wrong for an IPE: it carries its own IBBI recognition, and 10.1 already leans on
-- organisations being the umbrella an IP administers people under, so the two facts now have to
-- coexist on one row.
--
-- WHAT THIS MEANS FOR THE TWO AXES (V9). An IPE carries a pillar scope of IBBI, exactly as its
-- members do - it is IBBI that recognises it. Its member IPs carry *both* axes: the IPE as their
-- hierarchy node, and IBBI as their own scope, because each is individually registered and does not
-- stop being an IP by joining. That is the case the union in VisibilityScope exists for; an
-- intersection would have made a member visible only to someone holding both, and a sole
-- practitioner - an organisation of one - visible through neither.
--
-- ORGANISATION_PROFESSIONAL_ROLES MIRRORS THE USER TABLE rather than adding a flag. A boolean
-- "is_ipe" would record that the entity is an IP and lose the registration number that proves it,
-- which is the same mistake V5 corrected on the user side when it replaced four columns with a child
-- table. Reusing `professional_roles` and `professional_identification_types` keeps one vocabulary
-- for both kinds of principal, so a reader does not have to learn which of two catalogues applies.
--
-- No `legal_representative_type_id` here: Advocate/CA/CS qualifies a person, and an entity that
-- practises law is a legal entity rather than a legal representative.
--
-- REPAIRING V10 WHILE HERE. V10 seeded three organisations with legal_constitution
-- 'PRIVATE_LIMITED', which is not a LegalConstitution value - the enum has PRIVATE_LTD_COMPANY - so
-- @Enumerated(EnumType.STRING) threw on read and GET /api/v1/registrations/organisations/search
-- answered 500 for every caller. Fixed forward rather than by editing V10, which is already applied
-- and whose checksum must not move.

UPDATE organisations SET legal_constitution = 'PRIVATE_LTD_COMPANY'
 WHERE legal_constitution = 'PRIVATE_LIMITED';

ALTER TABLE organisations
    ADD COLUMN IF NOT EXISTS pillar_scope VARCHAR(10);

ALTER TABLE organisations
    DROP CONSTRAINT IF EXISTS chk_organisations_pillar_scope;
ALTER TABLE organisations
    ADD CONSTRAINT chk_organisations_pillar_scope
    CHECK (pillar_scope IS NULL OR pillar_scope IN ('IBBI', 'NCLT', 'NCLAT', 'MCA', 'NESL'));

CREATE INDEX IF NOT EXISTS idx_organisations_pillar_scope
    ON organisations (pillar_scope) WHERE pillar_scope IS NOT NULL;

CREATE TABLE IF NOT EXISTS organisation_professional_roles (
    id                                  UUID PRIMARY KEY,
    organisation_id                     UUID NOT NULL REFERENCES organisations (id) ON DELETE CASCADE,
    professional_role_id                UUID NOT NULL REFERENCES professional_roles (id),
    professional_identification_type_id UUID REFERENCES professional_identification_types (id),
    professional_identification_value   VARCHAR(100),
    created_at                          TIMESTAMPTZ NOT NULL,
    created_by                          VARCHAR(100) NOT NULL,
    updated_at                          TIMESTAMPTZ NOT NULL,
    updated_by                          VARCHAR(100) NOT NULL,
    version                             BIGINT      NOT NULL DEFAULT 0,
    is_active                           BOOLEAN     NOT NULL DEFAULT TRUE,
    deleted_at                          TIMESTAMPTZ,
    deleted_by                          VARCHAR(100),
    CONSTRAINT uq_organisation_professional_roles UNIQUE (organisation_id, professional_role_id)
);

CREATE INDEX IF NOT EXISTS idx_organisation_professional_roles_organisation_id
    ON organisation_professional_roles (organisation_id);

COMMENT ON TABLE organisation_professional_roles IS
    'Qualifications held by an organisation rather than a person. An IPE holds INSOLVENCY_'
    'PROFESSIONAL with its own IBBI recognition number - more than one IP together form an IPE and '
    'the IPE is itself considered an IP. Mirrors user_professional_roles deliberately.';
COMMENT ON COLUMN organisations.pillar_scope IS
    'The pillar that recognises this organisation - IBBI for an IPE. Null for an ordinary entity, '
    'which no pillar validated. Same axis as users.pillar_scope (V9).';

-- A demonstration IPE: an LLP, IBBI-recognised, with the two seeded insolvency professionals as its
-- members. It gives the hierarchy axis a case where the node is itself a principal, which no other
-- seeded organisation does.
INSERT INTO organisations (id, name, legal_constitution, id_type, id_value, registered_address,
                           contact_number, contact_email, created_at, created_by, updated_at, updated_by,
                           version, is_active, parent_id, level, pillar_scope)
VALUES ('50000000-0000-0000-0000-000000000004', 'Demo Insolvency Professional Entity LLP', 'LLP',
        'CIN', 'AAB-0001', '11 Resolution Street, Mumbai', '+91 9800000104', 'contact@demo-ipe.example.in',
        now(), 'flyway-seed', now(), 'flyway-seed', 0, true, NULL, NULL, 'IBBI')
ON CONFLICT (id) DO NOTHING;

INSERT INTO organisation_professional_roles (
    id, organisation_id, professional_role_id, professional_identification_type_id,
    professional_identification_value, created_at, created_by, updated_at, updated_by)
SELECT gen_random_uuid(), '50000000-0000-0000-0000-000000000004', pr.id,
       (SELECT id FROM professional_identification_types WHERE code = 'OTHER'),
       'IBBI/IPE/2026/0001', now(), 'flyway-seed', now(), 'flyway-seed'
  FROM professional_roles pr
 WHERE pr.code = 'INSOLVENCY_PROFESSIONAL'
ON CONFLICT (organisation_id, professional_role_id) DO NOTHING;

-- The members keep their own IBBI scope and gain the IPE as their hierarchy node: joining an IPE
-- does not end an individual's registration.
UPDATE users SET organisation_id = '50000000-0000-0000-0000-000000000004'
 WHERE email IN ('professional.demo@ipie.gov.in', 'multirole.demo@ipie.gov.in')
   AND organisation_id IS NULL;
