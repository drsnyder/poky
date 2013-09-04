CREATE OR REPLACE FUNCTION create_bucket_partition(_bucket text, _add_check BOOLEAN DEFAULT TRUE) RETURNS VOID AS
$$
DECLARE
  _child RECORD;
BEGIN

  --- FIXME: make this idempotent. if the bucket exists, just return
  EXECUTE format('SELECT c.relname AS child
              FROM pg_inherits
              JOIN pg_class AS c ON (inhrelid=c.oid)
              JOIN pg_class as p ON (inhparent=p.oid)
              WHERE p.relname = %L AND c.relname= %L', 'poky', 'poky_' || _bucket)
  INTO _child;

  IF (_child IS NOT NULL) THEN
    RAISE NOTICE 'Bucket % exists. Skipping.', _bucket;
  ELSE
    EXECUTE format(
      'CREATE TABLE poky_%I (
        PRIMARY KEY (key)
      ) INHERITS (poky)',
      _bucket);

    EXECUTE format('CREATE TRIGGER poky_%I_only_if_unmodified_since BEFORE UPDATE ON poky_%I
            FOR EACH ROW EXECUTE PROCEDURE only_if_unmodified_since()', _bucket, _bucket);

    IF (_add_check) THEN
      EXECUTE format('ALTER TABLE poky_%I ADD CONSTRAINT poky_%I_bucket_check CHECK (bucket = %L)', _bucket, _bucket, _bucket);
    END IF;

  END IF;

END
$$ LANGUAGE plpgsql;
