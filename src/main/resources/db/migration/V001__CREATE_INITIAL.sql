CREATE SEQUENCE  IF NOT EXISTS primary_sequence START WITH 10000 INCREMENT BY 1;

CREATE TABLE run_app_user (
    id BIGINT NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT run_app_user_pkey PRIMARY KEY (id)
);

CREATE TABLE garmin_run (
    id BIGINT NOT NULL,
    activity_id numeric(10, 2) NOT NULL,
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

CREATE TABLE shedlock (
    name BIGINT NOT NULL,
    lock_until TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    locked_by TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT shedlock_pkey PRIMARY KEY (name)
);

CREATE TABLE file_name_tracker (
    id BIGINT NOT NULL,
    file_name TEXT NOT NULL,
    updated_by VARCHAR(40),
    created_by_id BIGINT NOT NULL,
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
    runner_user_roles_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT runner_app_role_pkey PRIMARY KEY (id)
);

ALTER TABLE garmin_run ADD CONSTRAINT fk_garmin_run_created_by_id FOREIGN KEY (created_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE garmin_run ADD CONSTRAINT fk_garmin_run_update_by_id FOREIGN KEY (update_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE file_name_tracker ADD CONSTRAINT fk_file_name_tracker_created_by_id FOREIGN KEY (created_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE strava_run ADD CONSTRAINT fk_strava_run_created_by_id FOREIGN KEY (created_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE strava_run ADD CONSTRAINT fk_strava_run_updated_by_id FOREIGN KEY (updated_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE runner_app_role ADD CONSTRAINT fk_runner_app_role_runner_user_roles_id FOREIGN KEY (runner_user_roles_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE runner_app_role ADD CONSTRAINT unique_runner_app_role_role_name UNIQUE (role_name);
