BEGIN;
SET maintenance_work_mem = '256MB';
ALTER TABLE poky RENAME TO oldpoky;

\i sql/partitioned/poky.sql
\i sql/shared/stored_procedures.sql
\i sql/partitioned/stored_procedures.sql

ALTER TABLE poky DROP CONSTRAINT IF EXISTS modified_at_utc_check;
ALTER TABLE poky DROP CONSTRAINT IF EXISTS created_at_utc_check;

DO
$$
DECLARE
  _bucket TEXT;
  _inserted INTEGER;
BEGIN

    FOR _bucket IN SELECT DISTINCT bucket FROM oldpoky
    LOOP
      RAISE NOTICE 'Creating bucket %', _bucket;
      EXECUTE create_bucket_partition(_bucket, false);

      RAISE NOTICE 'Migrating data for bucket %...', _bucket;
      EXECUTE format('INSERT INTO poky_%I SELECT * FROM oldpoky WHERE bucket = %L', _bucket, _bucket);
      GET DIAGNOSTICS _inserted = ROW_COUNT;
      RAISE NOTICE 'Inserted % rows for bucket %', _inserted, _bucket;

      RAISE NOTICE 'Adding check bucket = %', _bucket;
      EXECUTE format('ALTER TABLE poky_%I ADD CONSTRAINT poky_%I_bucket_check CHECK (bucket = %L)', _bucket, _bucket, _bucket);
    END LOOP;

END;
$$ LANGUAGE plpgsql;

\i sql/shared/triggers.sql

\i sql/partitioned/triggers.sql

COMMIT;
--- remove oldpoky
