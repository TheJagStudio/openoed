CREATE TABLE app_user (
    id            uuid PRIMARY KEY,
    email         varchar(255) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    display_name  varchar(200) NOT NULL,
    role          varchar(32)  NOT NULL,
    art_unit      varchar(8),
    enabled       boolean      NOT NULL DEFAULT true,
    failed_logins integer      NOT NULL DEFAULT 0,
    locked_until  timestamptz,
    created_at    timestamptz  NOT NULL
);

CREATE TABLE refresh_token (
    id           uuid PRIMARY KEY,
    user_id      uuid         NOT NULL REFERENCES app_user (id),
    token_hash   varchar(64)  NOT NULL UNIQUE,
    expires_at   timestamptz  NOT NULL,
    revoked_at   timestamptz,
    created_at   timestamptz  NOT NULL
);
CREATE INDEX refresh_token_user_idx ON refresh_token (user_id);

CREATE TABLE trademark_application (
    id                   uuid PRIMARY KEY,
    serial_number        varchar(16)  NOT NULL UNIQUE,
    owner_id             uuid         NOT NULL REFERENCES app_user (id),
    mark                 varchar(200) NOT NULL,
    mark_type            varchar(32)  NOT NULL,
    international_class  integer      NOT NULL,
    art_unit             varchar(8)   NOT NULL,
    goods_description    varchar(4000) NOT NULL,
    owner_name           varchar(200) NOT NULL,
    owner_address        varchar(500) NOT NULL,
    owner_email          varchar(255) NOT NULL,
    attorney_name        varchar(200),
    attorney_bar_number  varchar(32),
    status               varchar(32)  NOT NULL,
    risk_score           integer,
    risk_band            varchar(16),
    filed_at             timestamptz  NOT NULL,
    updated_at           timestamptz  NOT NULL
);
CREATE INDEX application_owner_idx ON trademark_application (owner_id);
CREATE INDEX application_status_idx ON trademark_application (status);
CREATE INDEX application_band_idx ON trademark_application (risk_band);

CREATE TABLE specimen (
    id               uuid PRIMARY KEY,
    application_id   uuid         NOT NULL UNIQUE REFERENCES trademark_application (id),
    content_type     varchar(80)  NOT NULL,
    byte_size        integer      NOT NULL,
    sha256           varchar(64)  NOT NULL,
    storage_key      varchar(80)  NOT NULL,
    width            integer      NOT NULL,
    height           integer      NOT NULL,
    exif_present     boolean      NOT NULL,
    jpeg             boolean      NOT NULL
);
CREATE INDEX specimen_sha_idx ON specimen (sha256);

CREATE TABLE risk_assessment (
    id                uuid PRIMARY KEY,
    application_id    uuid         NOT NULL UNIQUE REFERENCES trademark_application (id),
    score             integer      NOT NULL,
    band              varchar(16)  NOT NULL,
    findings_json     text         NOT NULL,
    claude_status     varchar(16)  NOT NULL,
    claude_summary    varchar(4000),
    claude_suspicion  varchar(16),
    claude_flags_json text,
    created_at        timestamptz  NOT NULL
);

CREATE TABLE review_item (
    id              uuid PRIMARY KEY,
    application_id  uuid         NOT NULL UNIQUE REFERENCES trademark_application (id),
    status          varchar(16)  NOT NULL,
    priority        integer      NOT NULL,
    assignee_id     uuid,
    notes           varchar(4000),
    opened_at       timestamptz  NOT NULL,
    closed_at       timestamptz
);
CREATE INDEX review_status_idx ON review_item (status);

CREATE TABLE office_action (
    id                 uuid PRIMARY KEY,
    application_id     uuid         NOT NULL REFERENCES trademark_application (id),
    examiner_id        uuid         NOT NULL REFERENCES app_user (id),
    type               varchar(16)  NOT NULL,
    body               varchar(8000) NOT NULL,
    issued_at          timestamptz  NOT NULL,
    statutory_due_at   timestamptz  NOT NULL,
    schedule_fire_at   timestamptz  NOT NULL,
    schedule_name      varchar(80)  NOT NULL UNIQUE,
    status             varchar(16)  NOT NULL
);
CREATE INDEX office_action_app_idx ON office_action (application_id);

CREATE TABLE applicant_response (
    id               uuid PRIMARY KEY,
    office_action_id uuid         NOT NULL UNIQUE REFERENCES office_action (id),
    applicant_id     uuid         NOT NULL REFERENCES app_user (id),
    body             varchar(8000) NOT NULL,
    submitted_at     timestamptz  NOT NULL
);

CREATE TABLE notice (
    id                   uuid PRIMARY KEY,
    application_id       uuid         NOT NULL REFERENCES trademark_application (id),
    office_action_id     uuid         NOT NULL UNIQUE REFERENCES office_action (id),
    to_email             varchar(255) NOT NULL,
    subject              varchar(300) NOT NULL,
    body                 varchar(8000) NOT NULL,
    provider_message_id  varchar(80),
    sent_at              timestamptz  NOT NULL
);

CREATE TABLE prosecution_event (
    id              uuid PRIMARY KEY,
    application_id  uuid         NOT NULL REFERENCES trademark_application (id),
    kind            varchar(40)  NOT NULL,
    detail          varchar(2000) NOT NULL,
    actor_id        uuid,
    at              timestamptz  NOT NULL
);
CREATE INDEX prosecution_app_idx ON prosecution_event (application_id, at);

CREATE TABLE shell_address (
    id       uuid PRIMARY KEY,
    address  varchar(500) NOT NULL UNIQUE,
    note     varchar(300)
);

CREATE TABLE famous_mark (
    id    uuid PRIMARY KEY,
    mark  varchar(80) NOT NULL UNIQUE
);

CREATE TABLE attorney_watch (
    bar_number varchar(32) PRIMARY KEY,
    name       varchar(200) NOT NULL,
    standing   varchar(32)  NOT NULL
);

CREATE TABLE domain_outbox (
    id           uuid PRIMARY KEY,
    type         varchar(40)  NOT NULL,
    payload      text         NOT NULL,
    created_at   timestamptz  NOT NULL,
    delivered_at timestamptz
);

CREATE TABLE audit_outbox (
    id           uuid PRIMARY KEY,
    payload      text         NOT NULL,
    created_at   timestamptz  NOT NULL,
    delivered_at timestamptz
);

INSERT INTO shell_address (id, address, note) VALUES
    ('11111111-1111-1111-1111-111111111111', '1234 Market Street, Suite 400, Wilmington, DE 19801', 'Virtual office used on repeated shell filings'),
    ('22222222-2222-2222-2222-222222222222', '1209 Orange Street, Wilmington, DE 19801', 'Registered-agent address'),
    ('33333333-3333-3333-3333-333333333333', '251 Little Falls Drive, Wilmington, DE 19808', 'Registered-agent address'),
    ('44444444-4444-4444-4444-444444444444', '1013 Centre Road, Suite 403S, Wilmington, DE 19805', 'Virtual office');

INSERT INTO famous_mark (id, mark) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', 'NIKE'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', 'APPLE'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', 'STARBUCKS'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', 'DISNEY'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', 'TESLA');

INSERT INTO attorney_watch (bar_number, name, standing) VALUES
    ('99999', 'Morgan Shell', 'SUSPENDED'),
    ('88888', 'Riley Void', 'EXCLUDED');
