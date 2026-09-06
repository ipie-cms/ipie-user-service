-- Reduces the professional-role catalogue to the qualifications it was always meant to hold, and
-- adds the one the code has been depending on since V5 without it ever existing.
--
-- THE CATALOGUE HELD THREE DIFFERENT AXES AT ONCE (ARCHITECTURE_WORKING_PLAN.md 10.1, 10.5). A professional
-- role answers what a person *is*, proved by a credential that outlives any case. Nine of the twelve
-- seeded codes answer something else:
--
--   Party to a case   FINANCIAL_CREDITOR, OPERATIONAL_CREDITOR, RESOLUTION_APPLICANT,
--                     GOVERNMENT_AUTHORITY, CORPORATE_DEBTOR
--   Capacity          LIQUIDATOR, AUTHORIZED_REPRESENTATIVE
--   Neither           PROFESSIONAL, OTHER, ADMIN
--
-- Being a financial creditor is a relationship to one corporate debtor, established by filing a
-- claim and classified by the resolution professional when the claim is verified (programme,
-- 2026-08-16). It is therefore not knowable at registration, which is where this table is filled:
-- whatever a registering user ticked was a guess the platform then stored as fact. Financial versus
-- operational follows from the nature of the debt, not from self-declaration, and government dues -
-- income tax, SEBI - are operational creditors rather than a participant type of their own.
-- CORPORATE_DEBTOR is the subject of a case, not a role a user holds. LIQUIDATOR and
-- AUTHORIZED_REPRESENTATIVE are capacities held on a case by appointment for an interval; 10.1
-- already records that AR does not belong here, which is why the multi-qualification demo account
-- deliberately does not hold it. ADMIN is a platform role and belongs to ipie-iam-service, which
-- already has PILLAR_ADMIN and SUPER_ADMIN - keeping it here would give a registering user a
-- way to claim it. This departs from the literal wording of FRS item 6, which lists Admin among the
-- professional roles, and does so knowingly (user, 2026-08-16).
--
-- LEGAL_REPRESENTATIVE IS ADDED, AND ITS ABSENCE WAS A LIVE DEFECT rather than a gap. V5 moved the
-- Advocate/CA/CS type onto the role row and RegistrationPolicy enforces that it may be set on
-- LEGAL_REPRESENTATIVE alone - resolving that role by code against this table. No row carried the
-- code, so the lookup returned null and every registration supplying a legal-representative type
-- failed on `Objects.equals(roleId, null)`, reporting "A legal representative type may only be set
-- on the LEGAL_REPRESENTATIVE role" - naming a role the wizard had never offered. The registration
-- wizard renders the dropdown from `legal_representative_types` regardless, so the whole branch -
-- lookup table, column, domain field, validation rule and UI control - was wired end to end to a
-- role that did not exist, and picking Advocate, CA or CS made a registration unsubmittable. FRS
-- item 6 names Legal Rep (Advocate/CA/CS) as one of the four.
--
-- DEACTIVATED, NOT DELETED. Existing registrations reference these rows - FINANCIAL_CREDITOR alone
-- is held by several - and deleting the lookup would either orphan them or destroy what a person
-- declared. `is_active = false` stops the code being offered while every historical row stays
-- readable, which is the same rule 10.3 states for assignments: closing is not deletion. The
-- registration lookup already filters on it (`findByActiveTrueOrderBySortOrderAsc`), so nothing
-- else has to change for the catalogue to shrink.
--
-- THE ID IS GENERATED, not a literal: V2 seeds this table with gen_random_uuid(), so reference-data
-- ids differ per environment and an id literal here would be correct on one database and dangling
-- on the next. Same reasoning as V6.

INSERT INTO professional_roles (id, code, label, sort_order, is_active)
VALUES (gen_random_uuid(), 'LEGAL_REPRESENTATIVE', 'Legal Representative', 3, true)
ON CONFLICT (code) DO UPDATE SET is_active = true, sort_order = 3;

-- Contiguous ordering over what remains, so the wizard does not present 1, 9, 3.
UPDATE professional_roles SET sort_order = 1 WHERE code = 'INSOLVENCY_PROFESSIONAL';
UPDATE professional_roles SET sort_order = 2 WHERE code = 'REGISTERED_VALUER';

UPDATE professional_roles
   SET is_active = false
 WHERE code NOT IN ('INSOLVENCY_PROFESSIONAL', 'REGISTERED_VALUER', 'LEGAL_REPRESENTATIVE');
