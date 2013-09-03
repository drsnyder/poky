SET maintenance_work_mem = '256MB';
ALTER TABLE poky RENAME TO oldpoky;

\i sql/partitioned/poky.sql

\i sql/shared/stored_procedures.sql
\i sql/shared/triggers.sql

\i sql/partitioned/stored_procedures.sql
\i sql/partitioned/triggers.sql

DO
$$
DECLARE
  _bucket TEXT;
BEGIN

    FOR _bucket IN SELECT DISTINCT bucket FROM oldpoky
    LOOP
      RAISE NOTICE 'Creating bucket %', _bucket;
      PERFORM create_bucket_partition(_bucket);
      RAISE NOTICE 'Migrating data...';
      PERFORM format('INSERT INTO poky_%I SELECT * FROM oldpoky WHERE bucket = %L', _bucket, _bucket);
    END LOOP;

END;
$$ LANGUAGE plpgsql;


--- remove oldpoky
