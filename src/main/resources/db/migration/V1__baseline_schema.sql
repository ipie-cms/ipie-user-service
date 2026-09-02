-- ipie-user-service schema.
--
-- This is a baseline, not a step: it declares the schema as it stands rather than replaying how it
-- was reached. It supersedes the 33 migrations that preceded the repository's first commit, whose
-- intermediate states existed only on development machines. Everything the earlier migrations
-- explained that still applies has been carried into the comments here and in the files beside it.
--
-- Ordering below is pg_dump's: tables, then constraints, then indexes, then comments. Column
-- comments are part of the schema and are relied on - they are where a reader learns that a column
-- named `_hash` never holds the value it is named for.

CREATE TABLE public.audit_trail (
    id uuid NOT NULL,
    event_type character varying(20) NOT NULL,
    action character varying(100) NOT NULL,
    entity_type character varying(100) NOT NULL,
    entity_id character varying(100),
    case_id character varying(100),
    actor_user_id character varying(100),
    source_ip character varying(45),
    service_name character varying(100) NOT NULL,
    comment character varying(500),
    old_value text,
    new_value text,
    correlation_id character varying(100),
    occurred_at timestamp with time zone NOT NULL,
    persisted_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.consent_notices (
    id uuid NOT NULL,
    code character varying(64) NOT NULL,
    version integer NOT NULL,
    effective_from timestamp with time zone NOT NULL,
    summary text NOT NULL,
    document_uri text,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

COMMENT ON TABLE public.consent_notices IS 'Each version of each consent notice. Referenced by user_consents; versions are never edited in place.';

COMMENT ON COLUMN public.consent_notices.document_uri IS 'Optional pointer to the full notice, when it lives outside this database. summary is what the person saw inline.';

CREATE TABLE public.identity_proof_types (
    id uuid NOT NULL,
    code character varying(50) NOT NULL,
    label character varying(150) NOT NULL,
    sort_order integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

CREATE TABLE public.legal_representative_types (
    id uuid NOT NULL,
    code character varying(50) NOT NULL,
    label character varying(150) NOT NULL,
    sort_order integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

CREATE TABLE public.organisations (
    id uuid NOT NULL,
    name character varying(200) NOT NULL,
    legal_constitution character varying(40) NOT NULL,
    id_type character varying(10) NOT NULL,
    id_value character varying(50) NOT NULL,
    msme boolean DEFAULT false NOT NULL,
    msme_type character varying(50),
    registered_address character varying(500),
    contact_number character varying(20),
    contact_email character varying(254),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(100) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    country character varying(100),
    state character varying(100),
    city character varying(100),
    pin character varying(10),
    district character varying(100),
    CONSTRAINT chk_organisations_id_type CHECK (((id_type)::text = ANY ((ARRAY['CIN'::character varying, 'PAN'::character varying, 'LLPIN'::character varying, 'TAN'::character varying, 'OTHER'::character varying])::text[])))
);

CREATE TABLE public.outbox_events (
    event_id character varying(64) NOT NULL,
    payload text NOT NULL,
    occurred_at timestamp with time zone NOT NULL,
    published_at timestamp with time zone
);

CREATE TABLE public.processed_events (
    event_id character varying(128) NOT NULL,
    processed_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.professional_identification_types (
    id uuid NOT NULL,
    code character varying(50) NOT NULL,
    label character varying(150) NOT NULL,
    sort_order integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

CREATE TABLE public.professional_roles (
    id uuid NOT NULL,
    code character varying(50) NOT NULL,
    label character varying(150) NOT NULL,
    sort_order integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

CREATE TABLE public.stakeholder_link_requests (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    stakeholder_type character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(100) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    CONSTRAINT chk_stakeholder_link_requests_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'COMPLETED'::character varying, 'EXPIRED'::character varying])::text[]))),
    CONSTRAINT chk_stakeholder_link_requests_type CHECK (((stakeholder_type)::text = ANY ((ARRAY['IBBI'::character varying, 'NCLT'::character varying, 'NCLAT'::character varying, 'MCA'::character varying, 'NESL'::character varying])::text[])))
);

CREATE TABLE public.stakeholder_links (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    stakeholder_type character varying(20) NOT NULL,
    external_stakeholder_id character varying(100) NOT NULL,
    external_username character varying(100) NOT NULL,
    linked_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(100) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    CONSTRAINT chk_stakeholder_links_type CHECK (((stakeholder_type)::text = ANY ((ARRAY['IBBI'::character varying, 'NCLT'::character varying, 'NCLAT'::character varying, 'MCA'::character varying, 'NESL'::character varying])::text[])))
);

CREATE TABLE public.user_consents (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    notice_id uuid NOT NULL,
    item character varying(50) NOT NULL,
    granted_at timestamp with time zone NOT NULL,
    withdrawn_at timestamp with time zone,
    source character varying(32) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

COMMENT ON TABLE public.user_consents IS 'One row per person per item per grant. Withdrawal sets withdrawn_at; a re-grant is a new row.';

COMMENT ON COLUMN public.user_consents.item IS 'The itemised thing consented to - a NotificationChannel today (EMAIL, SMS).';

COMMENT ON COLUMN public.user_consents.source IS 'Where the decision was made: REGISTRATION or PROFILE_UPDATE. Distinguishes an initial choice from a later change.';

CREATE TABLE public.users (
    id uuid NOT NULL,
    username character varying(64) NOT NULL,
    email character varying(254) NOT NULL,
    full_name character varying(200),
    phone_number character varying(20),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(100) NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_by character varying(100) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    registration_status character varying(20) DEFAULT 'VERIFIED'::character varying NOT NULL,
    keycloak_user_id uuid,
    verification_token_hash character varying(64),
    verification_token_expires_at timestamp with time zone,
    verified_at timestamp with time zone,
    is_active boolean DEFAULT true NOT NULL,
    deleted_at timestamp with time zone,
    deleted_by character varying(100),
    organisation_id uuid,
    notification_channels character varying(50) DEFAULT 'EMAIL'::character varying NOT NULL,
    category character varying(20),
    address_line1 character varying(500),
    address_line2 character varying(500),
    country character varying(100),
    state character varying(100),
    city character varying(100),
    pin character varying(10),
    professional_identification_value character varying(50),
    email_otp_code_hash character varying(64),
    email_otp_expires_at timestamp with time zone,
    email_verified_at timestamp with time zone,
    professional_role_id uuid,
    legal_representative_type_id uuid,
    professional_identification_type_id uuid,
    identity_proof_type_id uuid,
    email_otp_attempts integer DEFAULT 0 NOT NULL,
    email_otp_resend_count integer DEFAULT 0 NOT NULL,
    identity_proof_number_hash character varying(64),
    identity_proof_number_last4 character varying(4),
    CONSTRAINT chk_users_category CHECK (((category)::text = ANY ((ARRAY['INDIAN'::character varying, 'NRI'::character varying, 'FOREIGNER'::character varying])::text[]))),
    CONSTRAINT chk_users_registration_status CHECK (((registration_status)::text = ANY ((ARRAY['PRE_REGISTRATION'::character varying, 'PROVISIONING'::character varying, 'UNVERIFIED'::character varying, 'VERIFIED'::character varying])::text[]))),
    CONSTRAINT chk_users_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);

COMMENT ON COLUMN public.users.verification_token_hash IS 'HMAC-SHA256 (peppered) of the stakeholder-admin approval token. Never the token itself.';

COMMENT ON COLUMN public.users.email_otp_code_hash IS 'HMAC-SHA256 (peppered) of the registration email OTP. Never the code itself.';

COMMENT ON COLUMN public.users.email_otp_attempts IS 'Failed guesses against the current email OTP. Reset when a new code is issued.';

COMMENT ON COLUMN public.users.email_otp_resend_count IS 'Email OTPs ever issued for this registration. Never reset - this is what bounds the total guesses.';

COMMENT ON COLUMN public.users.identity_proof_number_hash IS 'Peppered HMAC-SHA256 of the identity proof number, for equality checks only. Never the number.';

COMMENT ON COLUMN public.users.identity_proof_number_last4 IS 'Last four digits, for display. The rest of the number is not stored anywhere (Aadhaar Act s.29).';

ALTER TABLE ONLY public.audit_trail
    ADD CONSTRAINT audit_trail_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.consent_notices
    ADD CONSTRAINT consent_notices_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.identity_proof_types
    ADD CONSTRAINT identity_proof_types_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.legal_representative_types
    ADD CONSTRAINT legal_representative_types_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.organisations
    ADD CONSTRAINT organisations_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.outbox_events
    ADD CONSTRAINT outbox_events_pkey PRIMARY KEY (event_id);

ALTER TABLE ONLY public.processed_events
    ADD CONSTRAINT processed_events_pkey PRIMARY KEY (event_id);

ALTER TABLE ONLY public.professional_identification_types
    ADD CONSTRAINT professional_identification_types_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.professional_roles
    ADD CONSTRAINT professional_roles_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.stakeholder_link_requests
    ADD CONSTRAINT stakeholder_link_requests_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.stakeholder_links
    ADD CONSTRAINT stakeholder_links_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.consent_notices
    ADD CONSTRAINT uq_consent_notices_code_version UNIQUE (code, version);

ALTER TABLE ONLY public.identity_proof_types
    ADD CONSTRAINT uq_identity_proof_types_code UNIQUE (code);

ALTER TABLE ONLY public.legal_representative_types
    ADD CONSTRAINT uq_legal_representative_types_code UNIQUE (code);

ALTER TABLE ONLY public.organisations
    ADD CONSTRAINT uq_organisations_id_type_value UNIQUE (id_type, id_value);

ALTER TABLE ONLY public.professional_identification_types
    ADD CONSTRAINT uq_professional_identification_types_code UNIQUE (code);

ALTER TABLE ONLY public.professional_roles
    ADD CONSTRAINT uq_professional_roles_code UNIQUE (code);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_username UNIQUE (username);

ALTER TABLE ONLY public.user_consents
    ADD CONSTRAINT user_consents_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

CREATE INDEX idx_audit_trail_correlation_id ON public.audit_trail USING btree (correlation_id) WHERE (correlation_id IS NOT NULL);

CREATE INDEX idx_audit_trail_entity_type_id ON public.audit_trail USING btree (entity_type, entity_id);

CREATE INDEX idx_organisations_name ON public.organisations USING btree (name);

CREATE INDEX idx_outbox_events_unpublished ON public.outbox_events USING btree (occurred_at) WHERE (published_at IS NULL);

CREATE INDEX idx_stakeholder_link_requests_user_id ON public.stakeholder_link_requests USING btree (user_id);

CREATE INDEX idx_stakeholder_links_user_id ON public.stakeholder_links USING btree (user_id);

CREATE INDEX idx_user_consents_user_active ON public.user_consents USING btree (user_id) WHERE (withdrawn_at IS NULL);

CREATE INDEX idx_user_consents_user_id ON public.user_consents USING btree (user_id);

CREATE INDEX idx_users_created_at_id ON public.users USING btree (created_at, id);

CREATE INDEX idx_users_email_lower ON public.users USING btree (lower((email)::text));

CREATE INDEX idx_users_keycloak_user_id ON public.users USING btree (keycloak_user_id) WHERE (keycloak_user_id IS NOT NULL);

CREATE INDEX idx_users_organisation_id ON public.users USING btree (organisation_id);

CREATE INDEX idx_users_status ON public.users USING btree (status);

CREATE UNIQUE INDEX uq_stakeholder_links_type_external_id ON public.stakeholder_links USING btree (stakeholder_type, external_stakeholder_id);

CREATE UNIQUE INDEX uq_stakeholder_links_user_type ON public.stakeholder_links USING btree (user_id, stakeholder_type);

CREATE UNIQUE INDEX uq_users_phone_number ON public.users USING btree (phone_number) WHERE (phone_number IS NOT NULL);

CREATE UNIQUE INDEX uq_users_verification_token_hash ON public.users USING btree (verification_token_hash) WHERE (verification_token_hash IS NOT NULL);

ALTER TABLE ONLY public.stakeholder_link_requests
    ADD CONSTRAINT stakeholder_link_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.stakeholder_links
    ADD CONSTRAINT stakeholder_links_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.user_consents
    ADD CONSTRAINT user_consents_notice_id_fkey FOREIGN KEY (notice_id) REFERENCES public.consent_notices(id);

ALTER TABLE ONLY public.user_consents
    ADD CONSTRAINT user_consents_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_identity_proof_type_id_fkey FOREIGN KEY (identity_proof_type_id) REFERENCES public.identity_proof_types(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_legal_representative_type_id_fkey FOREIGN KEY (legal_representative_type_id) REFERENCES public.legal_representative_types(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_organisation_id_fkey FOREIGN KEY (organisation_id) REFERENCES public.organisations(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_professional_identification_type_id_fkey FOREIGN KEY (professional_identification_type_id) REFERENCES public.professional_identification_types(id);

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_professional_role_id_fkey FOREIGN KEY (professional_role_id) REFERENCES public.professional_roles(id);
