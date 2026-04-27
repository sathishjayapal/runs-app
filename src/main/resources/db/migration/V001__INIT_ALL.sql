-- Consolidated Flyway migration for initial runs-app database schema
-- Combines V001, V002, V003, V006 into single migration

-- ============================================================================
-- SEQUENCES
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS primary_sequence START WITH 10000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS file_import_record_sequence START WITH 10000 INCREMENT BY 1;

-- ============================================================================
-- BASE TABLES (no dependencies)
-- ============================================================================
CREATE TABLE run_app_user (
    id BIGINT NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT run_app_user_pkey PRIMARY KEY (id)
);

-- ============================================================================
-- DEPENDENT TABLES (reference run_app_user)
-- ============================================================================
CREATE TABLE garmin_run (
    id BIGINT NOT NULL,
    activity_id TEXT NOT NULL,
    activity_date TEXT NOT NULL,
    activity_type TEXT NOT NULL,
    activity_name TEXT NOT NULL,
    activity_description TEXT,
    elapsed_time TEXT,
    distance TEXT NOT NULL,
    max_heart_rate TEXT,
    calories TEXT,
    created_by_id BIGINT NOT NULL,
    update_by_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT garmin_run_pkey PRIMARY KEY (id)
);

CREATE TABLE file_name_tracker (
    id BIGINT NOT NULL,
    file_name TEXT NOT NULL,
    created_by_id BIGINT NOT NULL,
    update_by_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT file_name_tracker_pkey PRIMARY KEY (id)
);

CREATE TABLE strava_run (
    run_number BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    run_name VARCHAR(100) NOT NULL,
    run_date date NOT NULL,
    miles INTEGER NOT NULL,
    start_location BIGINT NOT NULL,
    created_by_id BIGINT NOT NULL,
    updated_by_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT strava_run_pkey PRIMARY KEY (run_number)
);

CREATE TABLE runner_app_role (
    id BIGINT NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT runner_app_role_pkey PRIMARY KEY (id),
    CONSTRAINT unique_runner_app_role_role_name UNIQUE (role_name)
);

CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- ============================================================================
-- FOREIGN KEY CONSTRAINTS
-- ============================================================================
ALTER TABLE garmin_run ADD CONSTRAINT fk_garmin_run_created_by_id
    FOREIGN KEY (created_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE garmin_run ADD CONSTRAINT fk_garmin_run_update_by_id
    FOREIGN KEY (update_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE file_name_tracker ADD CONSTRAINT fk_file_name_tracker_created_by_id
    FOREIGN KEY (created_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE strava_run ADD CONSTRAINT fk_strava_run_created_by_id
    FOREIGN KEY (created_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE strava_run ADD CONSTRAINT fk_strava_run_updated_by_id
    FOREIGN KEY (updated_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

-- ============================================================================
-- JUNCTION TABLE FOR USER-ROLE RELATIONSHIP
-- ============================================================================
CREATE TABLE run_app_user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT run_app_user_roles_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_run_app_user_roles_user_id FOREIGN KEY (user_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT fk_run_app_user_roles_role_id FOREIGN KEY (role_id) REFERENCES runner_app_role (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

-- ============================================================================
-- FILE IMPORT RECORD TABLE (reconciliation and retry tracking)
-- ============================================================================
CREATE TABLE file_import_record (
    id BIGINT PRIMARY KEY DEFAULT nextval('file_import_record_sequence'),
    file_name TEXT NOT NULL UNIQUE,
    expected_row_count INTEGER NOT NULL,
    success_count INTEGER NOT NULL DEFAULT 0,
    failed_count INTEGER NOT NULL DEFAULT 0,
    skipped_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reconciliation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_details TEXT,
    reconciliation_report TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    last_retry_at TIMESTAMP,
    email_alert_sent_at TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_by_id BIGINT NOT NULL,
    CONSTRAINT fk_file_import_created_by FOREIGN KEY(created_by_id) REFERENCES run_app_user(id)
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================
CREATE INDEX idx_file_import_status ON file_import_record(status);
CREATE INDEX idx_file_import_reconciliation_status ON file_import_record(reconciliation_status);
CREATE INDEX idx_file_import_processed_at ON file_import_record(processed_at DESC);
CREATE INDEX idx_file_import_file_name ON file_import_record(file_name);
CREATE INDEX idx_file_import_retry_count ON file_import_record(retry_count);

-- ============================================================================
-- TABLE AND COLUMN COMMENTS
-- ============================================================================
COMMENT ON TABLE file_import_record IS 'Tracks CSV file imports from Google Drive with detailed reconciliation metrics and retry tracking';
COMMENT ON COLUMN file_import_record.file_name IS 'Name of the imported CSV file (unique)';
COMMENT ON COLUMN file_import_record.expected_row_count IS 'Total number of data rows in the CSV file (excluding header)';
COMMENT ON COLUMN file_import_record.success_count IS 'Number of rows successfully inserted into the database';
COMMENT ON COLUMN file_import_record.failed_count IS 'Number of rows that failed during processing';
COMMENT ON COLUMN file_import_record.skipped_count IS 'Number of rows that were skipped (e.g., duplicates)';
COMMENT ON COLUMN file_import_record.status IS 'Processing status: PENDING, PROCESSING, COMPLETE_SUCCESS, COMPLETE_WITH_FAILURES, FAILED, RETRY_IN_PROGRESS, QUARANTINED';
COMMENT ON COLUMN file_import_record.reconciliation_status IS 'Reconciliation result: PENDING, PASS, FAIL, PARTIAL_PASS';
COMMENT ON COLUMN file_import_record.failure_details IS 'JSON array of failed rows with error details';
COMMENT ON COLUMN file_import_record.reconciliation_report IS 'Detailed reconciliation report including row counts and status';
COMMENT ON COLUMN file_import_record.retry_count IS 'Number of retry attempts after initial partial failure';
COMMENT ON COLUMN file_import_record.last_retry_at IS 'Timestamp of the most recent retry attempt';
COMMENT ON COLUMN file_import_record.email_alert_sent_at IS 'Timestamp when failure alert email was sent (null if not sent)';
COMMENT ON COLUMN file_import_record.processed_at IS 'Timestamp when processing started';
COMMENT ON COLUMN file_import_record.completed_at IS 'Timestamp when processing completed (either success or failure)';
