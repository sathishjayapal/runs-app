ALTER TABLE garmin_run
    ADD CONSTRAINT unique_activity_id UNIQUE (activity_id);
