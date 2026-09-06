-- Reference data: the lookup rows the application reads at runtime.
--
-- Required in every environment. These are codes the platform's own logic and its API contracts
-- depend on, not sample content, so they belong in a migration rather than in a seeding script.

-- Identity proof types
INSERT INTO identity_proof_types (id, code, label, sort_order) VALUES
    (gen_random_uuid(), 'PAN', 'PAN Card', 1),
    (gen_random_uuid(), 'AADHAAR', 'Aadhaar Card', 2);

-- Legal representative types
INSERT INTO legal_representative_types (id, code, label, sort_order) VALUES
    (gen_random_uuid(), 'ADVOCATE', 'Advocate', 1),
    (gen_random_uuid(), 'CA', 'Chartered Accountant', 2),
    (gen_random_uuid(), 'CS', 'Company Secretary', 3);

-- Professional identification types
INSERT INTO professional_identification_types (id, code, label, sort_order) VALUES
    (gen_random_uuid(), 'PAN_CARD', 'PAN Card', 1),
    (gen_random_uuid(), 'BAR_REGISTRATION_NO', 'Bar Registration No.', 2),
    (gen_random_uuid(), 'ICAI_REGISTRATION_NO', 'ICAI Registration No.', 3),
    (gen_random_uuid(), 'ICSI_REGISTRATION_NO', 'ICSI Registration No.', 4),
    (gen_random_uuid(), 'OTHER', 'Other', 5);

-- Professional roles
INSERT INTO professional_roles (id, code, label, sort_order) VALUES
    (gen_random_uuid(), 'INSOLVENCY_PROFESSIONAL', 'Insolvency Professional', 1),
    (gen_random_uuid(), 'FINANCIAL_CREDITOR', 'Financial Creditor', 2),
    (gen_random_uuid(), 'OPERATIONAL_CREDITOR', 'Operational Creditor', 3),
    (gen_random_uuid(), 'RESOLUTION_APPLICANT', 'Resolution Applicant', 4),
    (gen_random_uuid(), 'LIQUIDATOR', 'Liquidator', 5),
    (gen_random_uuid(), 'GOVERNMENT_AUTHORITY', 'Government Authority', 6),
    (gen_random_uuid(), 'CORPORATE_DEBTOR', 'Corporate Debtor', 7),
    (gen_random_uuid(), 'AUTHORIZED_REPRESENTATIVE', 'Authorized Representative', 8),
    (gen_random_uuid(), 'REGISTERED_VALUER', 'Registered Valuer', 9),
    (gen_random_uuid(), 'PROFESSIONAL', 'Professional', 10),
    (gen_random_uuid(), 'ADMIN', 'Admin', 11),
    (gen_random_uuid(), 'OTHER', 'Others', 12);

-- Version 1 of the notification-channel consent notice.
--
-- PLACEHOLDER TEXT, deliberately marked as such: the wording people are actually shown is a legal
-- artefact, not a developer's to invent. Replace this row's summary - or insert a version 2 pointing
-- at the approved document and leave this one in place - before anything reaches a real user.
-- Existing consent rows keep referencing whichever version was in force when they were made.
INSERT INTO consent_notices (id, code, version, effective_from, summary, document_uri) VALUES (
    '00000000-0000-0000-0000-0000000c0001',
    'NOTIFICATION_CHANNELS',
    1,
    now(),
    'PLACEHOLDER - not legally reviewed. iPIE will use the contact details you provide to send you '
        || 'notifications about your registration and cases, through the channels you select. You may '
        || 'withdraw consent for any channel at any time from your profile.',
    NULL
);
