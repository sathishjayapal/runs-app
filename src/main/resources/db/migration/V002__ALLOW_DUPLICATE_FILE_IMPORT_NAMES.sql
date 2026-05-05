-- Allow re-importing files with the same name by removing the unique key on file_import_record.file_name
ALTER TABLE file_import_record
    DROP CONSTRAINT IF EXISTS file_import_record_file_name_key;

COMMENT ON COLUMN file_import_record.file_name IS 'Name of the imported CSV file (duplicates allowed for re-imports)';

