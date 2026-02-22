-- Sample Garmin run activities for testing

-- Clear existing test data to prevent duplicates
DELETE FROM garmin_run WHERE id >= 10001;

INSERT INTO garmin_run (
    id, activity_id, activity_date, activity_type, activity_name,
    activity_description, elapsed_time, distance, max_heart_rate,
    calories, created_by_id, update_by_id, created_at, updated_at
) VALUES
-- Run 1: Admin's morning run
(10007, 'GARMIN001', '2026-02-01', 'running', 'Morning 10K',
 'Tempo run with intervals', '00:52:30', '10.5', '175', '680',
 10004, NULL, NOW(), NOW()),

-- Run 2: Regular user's easy run
(10008, 'GARMIN002', '2026-02-05', 'running', 'Easy 5K',
 'Recovery run at easy pace', '00:30:15', '5.2', '145', '320',
 10005, NULL, NOW(), NOW()),

-- Run 3: Admin's strength training
(10009, 'GARMIN003', '2026-02-10', 'strength_training', 'Upper Body Workout',
 'Focus on chest and back', '00:45:00', '0.0', '130', '250',
 10004, 10004, NOW(), NOW()),

-- Run 4: Regular user's long run
(10010, 'GARMIN004', '2026-02-12', 'running', 'Sunday Long Run',
 'Half marathon distance', '01:58:45', '21.1', '165', '1450',
 10005, 10005, NOW(), NOW()),

-- Run 5: Admin's elliptical session
(10011, 'GARMIN005', '2026-02-15', 'elliptical', 'Cross Training',
 'Low impact cardio', '00:35:00', '4.5', '140', '280',
 10004, NULL, NOW(), NOW());
