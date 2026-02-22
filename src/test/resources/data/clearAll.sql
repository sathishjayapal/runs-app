-- Clear all test data in correct order (respecting foreign key constraints)

-- Clear runs first (they reference users)
DELETE FROM garmin_run;
DELETE FROM strava_run;

-- Clear file tracking
DELETE FROM file_name_tracker;

-- Clear user-role relationships
DELETE FROM run_app_user_roles;

-- Clear users and roles
DELETE FROM run_app_user;
DELETE FROM runner_app_role;

-- Reset sequences to start fresh
ALTER SEQUENCE IF EXISTS primary_sequence RESTART WITH 10000;
