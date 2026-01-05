ALTER TABLE users RENAME TO run_app_user;

ALTER TABLE run_app_user DROP COLUMN created_at;

ALTER TABLE garmin_runs RENAME TO garmin_run;

ALTER TABLE garmin_run ADD update_by_id BIGINT;

ALTER TABLE garmin_run DROP COLUMN created_at;

ALTER TABLE garmin_run DROP COLUMN updated_at;

ALTER TABLE file_name_tracker DROP COLUMN created_at;

ALTER TABLE file_name_tracker DROP COLUMN updated_at;

ALTER TABLE strava_runs RENAME TO strava_run;

ALTER TABLE strava_run ADD updated_by_id BIGINT;

ALTER TABLE strava_run DROP COLUMN created_at;

ALTER TABLE strava_run DROP COLUMN updated_at;

ALTER TABLE strava_run DROP COLUMN updated_by;

CREATE TABLE runner_app_role (
    id BIGINT NOT NULL,
    role_name VARCHAR(255) NOT NULL,
    runner_user_roles_id BIGINT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT runner_app_role_pkey PRIMARY KEY (id)
);

ALTER TABLE garmin_run ADD CONSTRAINT fk_garmin_run_update_by_id FOREIGN KEY (update_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE strava_run ALTER COLUMN  updated_by_id SET NOT NULL;

ALTER TABLE strava_run ADD CONSTRAINT fk_strava_run_updated_by_id FOREIGN KEY (updated_by_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE runner_app_role ADD CONSTRAINT fk_runner_app_role_runner_user_roles_id FOREIGN KEY (runner_user_roles_id) REFERENCES run_app_user (id) ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE runner_app_role ADD CONSTRAINT unique_runner_app_role_role_name UNIQUE (role_name);
