-- A demonstration account holding two professional qualifications at once.
--
-- Every other seeded demo account holds exactly one role, because that was all the schema allowed
-- until V5. Nothing seeded therefore exercises the case V5 exists for, and the first person to open
-- the UI after that change sees a system that still looks single-role. This account is the fixture
-- that shows otherwise.
--
-- WHY THESE TWO ROLES AND NOT IRP, RP, LIQUIDATOR OR AR. This table holds what a person *is* - a
-- qualification, proved by a registration number. It does not hold what they *act as*, which is
-- decided per case by an NCLT order: the same IP is IRP on one matter, RP on another once the CoC
-- resolves, Liquidator on a third, and can be replaced on any of them without any of this changing.
-- Those are capacities on an assignment (ARCHITECTURE_WORKING_PLAN.md 10.1) and belong to the case model,
-- which does not exist yet. Seeding one here would record a case-scoped fact as a standing
-- qualification and teach every reader the wrong shape.
--
-- Insolvency Professional and Registered Valuer are both genuine qualifications, and each carries a
-- registration issued under its own regulation. That is the case the child table exists for: one
-- column on users could hold one number or the other, never both, and choosing between them loses
-- the ability to prove either.
--
-- LOOKUPS ARE RESOLVED BY CODE, not by id: reference-data ids are generated per environment
-- (V2 uses gen_random_uuid()), so an id literal here would be correct on one database and dangling
-- on the next.
--
-- THE IDENTIFICATION TYPE IS 'OTHER' FOR BOTH, and that is a gap rather than a choice. The
-- catalogue seeded in V2 offers PAN_CARD, BAR_REGISTRATION_NO, ICAI_REGISTRATION_NO,
-- ICSI_REGISTRATION_NO and OTHER - it has no entry for an IBBI registration, which is the one every
-- IP, RV and AR actually carries. Worth correcting in reference data; not corrected here, because
-- this file seeds a demo account and should not quietly redefine the platform's lookup values.
--
-- LIKE V3, THIS MUST NOT REACH PRODUCTION. It is a demonstration fixture and belongs in a
-- development-only Flyway location once the platform grows one.

INSERT INTO users (
    id, username, email, full_name, phone_number, status,
    created_at, created_by, updated_at, updated_by, version,
    registration_status, keycloak_user_id, verified_at, is_active, notification_channels,
    email_otp_attempts, email_otp_resend_count)
VALUES (
    '10000000-0000-0000-0000-000000000011',
    'multirole.demo@ipie.gov.in', 'multirole.demo@ipie.gov.in',
    'Demo Multi-Role Professional', '+91 9800000011', 'ACTIVE',
    now(), 'flyway-seed', now(), 'flyway-seed', 0,
    'VERIFIED', '20000000-0000-0000-0000-000000000012', now(), true, 'EMAIL',
    0, 0);

INSERT INTO user_professional_roles (
    id, user_id, professional_role_id, professional_identification_type_id,
    professional_identification_value, created_at, created_by, updated_at, updated_by)
SELECT gen_random_uuid(),
       '10000000-0000-0000-0000-000000000011',
       pr.id,
       (SELECT id FROM professional_identification_types WHERE code = 'OTHER'),
       v.identification_value,
       now(), 'flyway-seed', now(), 'flyway-seed'
  FROM (VALUES
        ('INSOLVENCY_PROFESSIONAL', 'IBBI/IPA-001/IP-P00123/2026-2027/13456'),
        ('REGISTERED_VALUER',       'IBBI/RV/06/2026/12345')
       ) AS v(role_code, identification_value)
  JOIN professional_roles pr ON pr.code = v.role_code;
