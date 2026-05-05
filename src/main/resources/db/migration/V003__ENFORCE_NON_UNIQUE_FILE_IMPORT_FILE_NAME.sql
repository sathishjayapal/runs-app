-- Enforce non-unique file_name for file_import_record even if recreated outside Flyway
ALTER TABLE public.file_import_record
    DROP CONSTRAINT IF EXISTS file_import_record_file_name_key;

DO $$
DECLARE
    idx RECORD;
BEGIN
    FOR idx IN
        SELECT indexname
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'file_import_record'
          AND indexdef ILIKE 'CREATE UNIQUE INDEX%'
          AND indexdef ILIKE '%(file_name)%'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS public.%I', idx.indexname);
    END LOOP;
END $$;

COMMENT ON COLUMN public.file_import_record.file_name IS 'Name of the imported CSV file (duplicates allowed for re-imports)';

