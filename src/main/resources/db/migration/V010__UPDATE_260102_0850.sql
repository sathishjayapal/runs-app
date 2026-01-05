ALTER TABLE users RENAME TO "user";

ALTER TABLE "user" DROP COLUMN created_at;

ALTER TABLE garmin_runs RENAME TO garmin_run;

ALTER TABLE garmin_run DROP COLUMN created_at;

ALTER TABLE garmin_run DROP COLUMN updated_at;

ALTER TABLE file_name_tracker DROP COLUMN created_at;

ALTER TABLE file_name_tracker DROP COLUMN updated_at;

ALTER TABLE strava_runs RENAME TO strava_run;

ALTER TABLE strava_run DROP COLUMN created_at;

ALTER TABLE strava_run DROP COLUMN updated_at;
