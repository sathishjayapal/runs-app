-- Sample file tracker data for testing

-- Clear existing test data to prevent duplicates
DELETE FROM file_name_tracker WHERE id >= 30001;

INSERT INTO file_name_tracker (
    id, file_name, created_by_id, update_by_id, created_at, updated_at
) VALUES
-- Admin's tracked files
(30001, 'morning-run-2026-02-01.fit', 10004, NULL, NOW(), NOW()),
(30002, 'evening-run-2026-02-05.fit', 10004, NULL, NOW(), NOW()),
(30003, 'workout-2026-02-10.fit', 10004, 10004, NOW(), NOW()),

-- Regular user's tracked files
(30004, 'my-run-2026-02-12.fit', 10005, NULL, NOW(), NOW()),
(30005, 'trail-run-2026-02-15.fit', 10005, NULL, NOW(), NOW());
