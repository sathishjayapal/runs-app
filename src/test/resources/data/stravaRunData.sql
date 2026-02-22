-- Sample Strava run activities for testing

-- Clear existing test data to prevent duplicates
DELETE FROM strava_run WHERE run_number >= 20001;

INSERT INTO strava_run (
    run_number, customer_id, run_name, run_date, miles,
    start_location, created_by_id, updated_by_id, created_at, updated_at
) VALUES
-- Run 1: Admin's Strava run
(20001, 100, 'Morning Trail Run', '2026-02-03', 8, 1, 10004, NULL, NOW(), NOW()),

-- Run 2: Regular user's Strava run
(20002, 101, 'Evening Track Workout', '2026-02-07', 5, 2, 10005, NULL, NOW(), NOW()),

-- Run 3: Admin's long Strava run
(20003, 100, 'Weekend Long Run', '2026-02-14', 15, 3, 10004, 10004, NOW(), NOW());
