-- The two axes a user can be placed on, both optional, from the programme's description on
-- 2026-08-17. Neither is mandatory because most principals sit on one axis and not the other.
--
--   HIERARCHY (organisations.parent_id, users.organisation_id) - who a user sits *under*. An entity
--   admin is the root of their own tree and has no parent; the users they invite carry the entity as
--   their node. A parent sees everything beneath it; siblings see nothing of each other.
--
--   SCOPE (users.pillar_scope) - which pillar *validates* the principal. An insolvency professional
--   is scoped to IBBI because their standing rests on an IBBI registration number, and they sit in
--   no hierarchy at all: hierarchy null, scope IBBI. An entity admin is the mirror image: hierarchy
--   root, scope null, because no pillar validated them.
--
-- WHY THE UMBRELLA IS AN ORGANISATION AND NOT A USER. An IP who administers people under their own
-- practice needs something a row can point at, and 10.1 already settled what: an IPE is an
-- organisation, and a sole practitioner is an organisation of one. Pointing at an organisation in
-- every case avoids a polymorphic parent that is sometimes a user and sometimes an entity - which no
-- foreign key can express and every query would have to branch on. `users.organisation_id` already
-- exists and becomes that pointer; this migration only gives organisations a parent so the pointer
-- can describe a tree rather than a flat list.
--
-- THE SAME COLUMN MEANS TWO THINGS DEPENDING ON THE ROLE HELD, deliberately. `pillar_scope` on a
-- user holding PILLAR_ADMIN is the pillar they *administer*; on anyone else it is the pillar that
-- validated them. One column, because a pillar admin at IBBI is also an IBBI principal, and two
-- columns would let those disagree.
--
-- LEVEL IS FOR THE FRS ITEM 9 LADDER (country/regional/zonal/state/branch), recorded now because the
-- hierarchy is being created here and adding it later means backfilling every row. Visibility does
-- not read it: cascading upward "along direct ancestry" is answered by the parent chain, not by
-- comparing levels, and two branches may sit at the same level without seeing each other.

ALTER TABLE organisations
    ADD COLUMN IF NOT EXISTS parent_id UUID REFERENCES organisations (id),
    ADD COLUMN IF NOT EXISTS level     VARCHAR(20);

ALTER TABLE organisations
    DROP CONSTRAINT IF EXISTS chk_organisations_level;
ALTER TABLE organisations
    ADD CONSTRAINT chk_organisations_level
    CHECK (level IS NULL OR level IN ('COUNTRY', 'REGIONAL', 'ZONAL', 'STATE', 'BRANCH'));

-- A node may not be its own parent. Deeper cycles are prevented in the service, which is where the
-- whole chain is visible; a single-row check is what SQL can enforce cheaply on every write.
ALTER TABLE organisations
    DROP CONSTRAINT IF EXISTS chk_organisations_not_own_parent;
ALTER TABLE organisations
    ADD CONSTRAINT chk_organisations_not_own_parent CHECK (parent_id IS NULL OR parent_id <> id);

CREATE INDEX IF NOT EXISTS idx_organisations_parent_id ON organisations (parent_id);

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS pillar_scope VARCHAR(10);

-- The five pillars, as PillarType. A CHECK rather than a lookup table: the set is fixed by statute
-- and by the identity federation built against it, and a sixth would need code in the SPI anyway.
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS chk_users_pillar_scope;
ALTER TABLE users
    ADD CONSTRAINT chk_users_pillar_scope
    CHECK (pillar_scope IS NULL OR pillar_scope IN ('IBBI', 'NCLT', 'NCLAT', 'MCA', 'NESL'));

-- Both axes are filtered on for every list a scoped administrator sees, so both are indexed. The
-- partial index skips the rows that carry no scope, which today is most of them.
CREATE INDEX IF NOT EXISTS idx_users_pillar_scope   ON users (pillar_scope) WHERE pillar_scope IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_organisation_id ON users (organisation_id) WHERE organisation_id IS NOT NULL;

COMMENT ON COLUMN users.pillar_scope IS
    'The pillar that validates this principal (IBBI for an insolvency professional or registered '
    'valuer, by registration number), or the pillar administered when the user holds PILLAR_ADMIN. '
    'Null for anyone no pillar validated - an entity admin and the users beneath them.';
COMMENT ON COLUMN organisations.parent_id IS
    'The node this organisation sits under. Null at the root of a tree. Visibility cascades upward '
    'along direct ancestry only, so siblings never see each other (FRS item 9).';
