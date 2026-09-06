-- Places the seeded accounts on the two axes V9 added, so the visibility rule is demonstrable
-- rather than merely implemented. Without this every user has null on both axes, every scoped
-- administrator sees only themselves, and a passing test proves nothing about the query.
--
-- The shape mirrors what the programme described on 2026-08-17:
--
--   IBBI (pillar)          -> the insolvency professionals and registered valuers it validates,
--                             which sit in no hierarchy at all
--   Demo Entity (root)     -> Demo Entity Branch (child)
--                             each with its own users, so sibling isolation has something to isolate
--
-- LIKE V3 AND V6, THIS MUST NOT REACH PRODUCTION. It is demonstration data.
--
-- Ids are literals here, unlike the reference data in V2: these rows are referenced by
-- ipie-iam-service's seeds and by the end-to-end run, which cannot look them up by a natural key
-- that does not exist.

INSERT INTO organisations (id, name, legal_constitution, id_type, id_value, registered_address,
                           contact_number, contact_email, created_at, created_by, updated_at, updated_by,
                           version, is_active, parent_id, level)
VALUES
    ('50000000-0000-0000-0000-000000000001', 'Demo Entity', 'PRIVATE_LIMITED', 'CIN', 'U00000MH2020PTC000001',
     '1 Demo Road, Mumbai', '+91 9800000101', 'contact@demo-entity.example.in',
     now(), 'flyway-seed', now(), 'flyway-seed', 0, true, NULL, 'COUNTRY'),
    ('50000000-0000-0000-0000-000000000002', 'Demo Entity Branch', 'PRIVATE_LIMITED', 'CIN', 'U00000MH2020PTC000002',
     '2 Demo Road, Pune', '+91 9800000102', 'branch@demo-entity.example.in',
     now(), 'flyway-seed', now(), 'flyway-seed', 0, true, '50000000-0000-0000-0000-000000000001', 'STATE'),
    -- A second root, deliberately unrelated. It is what proves the rule is a subtree and not simply
    -- "any organisation": nothing in the Demo Entity tree may ever see this one's users.
    ('50000000-0000-0000-0000-000000000003', 'Rival Entity', 'PRIVATE_LIMITED', 'CIN', 'U00000MH2020PTC000003',
     '9 Rival Road, Delhi', '+91 9800000103', 'contact@rival-entity.example.in',
     now(), 'flyway-seed', now(), 'flyway-seed', 0, true, NULL, 'COUNTRY')
ON CONFLICT (id) DO NOTHING;

-- The professionals IBBI validates. They carry a pillar and no organisation, which is the case the
-- hierarchy axis alone cannot express and the reason the two axes are OR-ed rather than AND-ed.
UPDATE users SET pillar_scope = 'IBBI'
 WHERE email IN ('professional.demo@ipie.gov.in', 'multirole.demo@ipie.gov.in', 'rv.demo@ipie.gov.in')
   AND pillar_scope IS NULL;

-- The pillar administrator's own scope: for a PILLAR_ADMIN holder this column is the pillar they
-- administer rather than the one that validated them (V9's comment on the column).
UPDATE users SET pillar_scope = 'IBBI'
 WHERE email = 'admin@ipie.gov.in' AND pillar_scope IS NULL;

-- The entity side. The creditor demo account roots the Demo Entity tree; the legal representative
-- sits in its branch, so the root sees both and the branch sees only itself.
UPDATE users SET organisation_id = '50000000-0000-0000-0000-000000000001'
 WHERE email = 'creditor.demo@ipie.gov.in' AND organisation_id IS NULL;
UPDATE users SET organisation_id = '50000000-0000-0000-0000-000000000002'
 WHERE email = 'legalrep.demo@ipie.gov.in' AND organisation_id IS NULL;
UPDATE users SET organisation_id = '50000000-0000-0000-0000-000000000003'
 WHERE email = 'authorizedrep.demo@ipie.gov.in' AND organisation_id IS NULL;
