CREATE SEQUENCE  IF NOT EXISTS primary_sequence START WITH 10000 INCREMENT BY 1;

CREATE TABLE users (
    id BIGINT NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

CREATE TABLE garmin_runs (
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
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by VARCHAR(40),
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT garmin_runs_pkey PRIMARY KEY (id)
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
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by VARCHAR(40),
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT file_name_tracker_pkey PRIMARY KEY (id)
);

CREATE TABLE strava_runs (
    run_number BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    run_name VARCHAR(100) NOT NULL,
    run_date date NOT NULL,
    miles INTEGER NOT NULL,
    start_location BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_by VARCHAR(20),
    created_by_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT strava_runs_pkey PRIMARY KEY (run_number)
);

ALTER TABLE garmin_runs ADD CONSTRAINT fk_garmin_runs_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE file_name_tracker ADD CONSTRAINT fk_file_name_tracker_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE strava_runs ADD CONSTRAINT fk_strava_runs_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON UPDATE NO ACTION ON DELETE NO ACTION;
