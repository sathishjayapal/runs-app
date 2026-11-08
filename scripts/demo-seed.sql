-- Synthetic demo data for the local runs-app demo (scripts/demo-up.sh). Entirely fictional:
-- generated, not exported from any Garmin or Strava account. Safe to screenshot or screen-record.
-- Idempotent: re-running adds nothing new (garmin_run.activity_id is unique; Strava rows are skipped
-- when the demo marker rows already exist).
-- Dates are relative to today, so the demo always looks recent.

INSERT INTO garmin_run
    (id, activity_id, activity_date, activity_type, activity_name, activity_description,
     elapsed_time, distance, max_heart_rate, calories, created_by_id, created_at, updated_at)
SELECT nextval('primary_sequence'), v.activity_id,
       to_char((current_date - v.days_ago) + v.start_time::time, 'YYYY-MM-DD"T"HH24:MI:SS"Z"'),
       'Running', v.activity_name, v.description, v.elapsed_time, v.distance, v.max_hr, v.calories,
       u.id, now(), now()
FROM (VALUES
    ('demo-0001', 84, '06:40', 'Park Loop Easy', 'Conversational pace, flat route.', '00:33:27', '5.6 km', '154', '328'),
    ('demo-0002', 82, '17:30', 'Tempo Session', 'Warm-up, steady hard effort, cool-down.', '00:38:55', '7.3 km', '192', '435'),
    ('demo-0003', 80, '07:10', 'Recovery Jog', 'Very easy, legs only.', '00:27:03', '4.2 km', '140', '262'),
    ('demo-0004', 79, '08:00', 'Endurance Long Run', 'Steady aerobic effort, fuel every 40 minutes.', '01:15:10', '12.4 km', '165', '781'),
    ('demo-0005', 77, '06:40', 'Neighborhood Easy Run', 'Conversational pace, flat route.', '00:42:42', '6.9 km', '165', '402'),
    ('demo-0006', 75, '17:30', 'Tempo Along the River', 'Warm-up, steady hard effort, cool-down.', '00:38:38', '7.1 km', '185', '435'),
    ('demo-0007', 73, '07:10', 'Short Recovery Run', 'Very easy, legs only.', '00:24:21', '3.7 km', '143', '217'),
    ('demo-0008', 72, '08:00', 'Endurance Long Run', 'Steady aerobic effort, fuel every 40 minutes.', '01:17:47', '12.4 km', '160', '775'),
    ('demo-0009', 70, '06:40', 'Steady Easy 5K', 'Conversational pace, flat route.', '00:33:41', '5.4 km', '166', '346'),
    ('demo-0010', 68, '17:30', 'Threshold Intervals', 'Warm-up, steady hard effort, cool-down.', '00:46:28', '8.8 km', '185', '527'),
    ('demo-0011', 66, '07:10', 'Gentle Recovery Run', 'Very easy, legs only.', '00:29:27', '4.5 km', '156', '271'),
    ('demo-0012', 65, '08:00', 'Long Run - Lakeside', 'Steady aerobic effort, fuel every 40 minutes.', '01:27:11', '13.7 km', '167', '861'),
    ('demo-0013', 63, '06:40', 'Steady Easy 5K', 'Conversational pace, flat route.', '00:31:46', '5.2 km', '163', '307'),
    ('demo-0014', 61, '17:30', 'Tempo Along the River', 'Warm-up, steady hard effort, cool-down.', '00:45:31', '8.3 km', '178', '532'),
    ('demo-0015', 59, '07:10', 'Gentle Recovery Run', 'Very easy, legs only.', '00:32:23', '4.7 km', '148', '298'),
    ('demo-0016', 58, '08:00', 'Long Run With Hills', 'Steady aerobic effort, fuel every 40 minutes.', '01:26:34', '13.9 km', '160', '911'),
    ('demo-0017', 56, '06:40', 'Steady Easy 5K', 'Conversational pace, flat route.', '00:37:58', '6.4 km', '162', '404'),
    ('demo-0018', 54, '17:30', 'Progression Run', 'Warm-up, steady hard effort, cool-down.', '00:41:30', '7.9 km', '187', '459'),
    ('demo-0019', 52, '07:10', 'Easy Recovery Spin', 'Very easy, legs only.', '00:27:03', '4.0 km', '153', '233'),
    ('demo-0020', 51, '08:00', 'Long Run With Hills', 'Steady aerobic effort, fuel every 40 minutes.', '01:25:43', '14.0 km', '170', '914'),
    ('demo-0021', 49, '06:40', 'Neighborhood Easy Run', 'Conversational pace, flat route.', '00:31:50', '5.2 km', '170', '313'),
    ('demo-0022', 47, '17:30', 'Cruise Intervals 4x1.5K', 'Warm-up, steady hard effort, cool-down.', '00:51:43', '9.5 km', '184', '604'),
    ('demo-0023', 45, '07:10', 'Short Recovery Run', 'Very easy, legs only.', '00:29:49', '4.5 km', '145', '266'),
    ('demo-0024', 44, '08:00', 'Endurance Long Run', 'Steady aerobic effort, fuel every 40 minutes.', '01:32:24', '14.6 km', '158', '903'),
    ('demo-0025', 42, '06:40', 'Sunrise Shakeout', 'Conversational pace, flat route.', '00:32:37', '5.4 km', '157', '331'),
    ('demo-0026', 40, '17:30', 'Tempo Along the River', 'Warm-up, steady hard effort, cool-down.', '00:46:00', '8.8 km', '180', '559'),
    ('demo-0027', 38, '07:10', 'Gentle Recovery Run', 'Very easy, legs only.', '00:33:17', '4.9 km', '139', '302'),
    ('demo-0028', 37, '08:00', 'Long Run - Lakeside', 'Steady aerobic effort, fuel every 40 minutes.', '01:36:03', '15.5 km', '173', '977'),
    ('demo-0029', 35, '06:40', 'Sunrise Shakeout', 'Conversational pace, flat route.', '00:34:31', '5.4 km', '167', '320'),
    ('demo-0030', 33, '17:30', 'Tempo Session', 'Warm-up, steady hard effort, cool-down.', '00:45:14', '8.8 km', '194', '521'),
    ('demo-0031', 31, '07:10', 'Gentle Recovery Run', 'Very easy, legs only.', '00:33:09', '4.9 km', '140', '318'),
    ('demo-0032', 30, '08:00', 'Long Run With Hills', 'Steady aerobic effort, fuel every 40 minutes.', '01:36:47', '15.8 km', '169', '992'),
    ('demo-0033', 28, '06:40', 'Sunrise Shakeout', 'Conversational pace, flat route.', '00:32:53', '5.2 km', '167', '321'),
    ('demo-0034', 26, '17:30', 'Progression Run', 'Warm-up, steady hard effort, cool-down.', '00:37:31', '7.3 km', '186', '466'),
    ('demo-0035', 24, '07:10', 'Easy Recovery Spin', 'Very easy, legs only.', '00:30:32', '4.7 km', '138', '280'),
    ('demo-0036', 23, '08:00', 'Long Run - Country Roads', 'Steady aerobic effort, fuel every 40 minutes.', '01:42:51', '16.4 km', '158', '1050'),
    ('demo-0037', 21, '06:40', 'Neighborhood Easy Run', 'Conversational pace, flat route.', '00:44:19', '7.0 km', '161', '435'),
    ('demo-0038', 19, '17:30', 'Cruise Intervals 4x1.5K', 'Warm-up, steady hard effort, cool-down.', '00:42:01', '8.1 km', '193', '520'),
    ('demo-0039', 17, '07:10', 'Gentle Recovery Run', 'Very easy, legs only.', '00:30:27', '4.5 km', '144', '290'),
    ('demo-0040', 16, '08:00', 'Long Run - Lakeside', 'Steady aerobic effort, fuel every 40 minutes.', '01:48:12', '17.7 km', '174', '1096'),
    ('demo-0041', 14, '06:40', 'Park Loop Easy', 'Conversational pace, flat route.', '00:44:03', '7.0 km', '168', '420'),
    ('demo-0042', 12, '17:30', 'Cruise Intervals 4x1.5K', 'Warm-up, steady hard effort, cool-down.', '00:52:15', '9.9 km', '187', '649'),
    ('demo-0043', 10, '07:10', 'Short Recovery Run', 'Very easy, legs only.', '00:23:15', '3.6 km', '153', '214'),
    ('demo-0044', 9, '08:00', 'Long Run With Hills', 'Steady aerobic effort, fuel every 40 minutes.', '01:56:52', '18.0 km', '177', '1165'),
    ('demo-0045', 7, '06:40', 'Easy Morning Loop', 'Conversational pace, flat route.', '00:41:17', '6.8 km', '155', '439'),
    ('demo-0046', 5, '17:30', 'Threshold Intervals', 'Warm-up, steady hard effort, cool-down.', '00:52:30', '9.7 km', '182', '599'),
    ('demo-0047', 3, '07:10', 'Gentle Recovery Run', 'Very easy, legs only.', '00:28:28', '4.2 km', '140', '270'),
    ('demo-0048', 2, '08:00', 'Steady Long Run', 'Steady aerobic effort, fuel every 40 minutes.', '01:58:30', '18.6 km', '160', '1186')
) AS v(activity_id, days_ago, start_time, activity_name, description, elapsed_time, distance, max_hr, calories)
CROSS JOIN (SELECT id FROM run_app_user WHERE email = 'runner@runsapp.com') AS u
ON CONFLICT (activity_id) DO NOTHING;

INSERT INTO strava_run
    (run_number, customer_id, run_name, run_date, miles, start_location, created_by_id, created_at, updated_at)
SELECT nextval('primary_sequence'), 1001, v.run_name, current_date - v.days_ago, v.miles,
       12340 + v.days_ago, u.id, now(), now()
FROM (VALUES
    ('Social Group Run', 6, 1),
    ('Trail Day - Ridge Loop', 9, 1),
    ('Local 5K Time Trial', 3, 2),
    ('Hill Session', 5, 3),
    ('Lunch Break Run', 4, 4),
    ('Long Run With Friends', 13, 6),
    ('Evening Easy Miles', 5, 9),
    ('Race Pace Practice', 8, 12)
) AS v(run_name, miles, days_ago)
CROSS JOIN (SELECT id FROM run_app_user WHERE email = 'runner@runsapp.com') AS u
WHERE NOT EXISTS (SELECT 1 FROM strava_run WHERE run_name = 'Social Group Run' AND customer_id = 1001);
