CREATE OR REPLACE FUNCTION create_bucket_partition(_bucket text) RETURNS VOID AS
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
        PRIMARY KEY (key),
        CHECK (bucket::text = %L)
      ) INHERITS (poky)',
      _bucket, _bucket);

    EXECUTE format('CREATE TRIGGER poky_%I_only_if_unmodified_since BEFORE UPDATE ON poky_%I
            FOR EACH ROW EXECUTE PROCEDURE only_if_unmodified_since()', _bucket, _bucket);
  END IF;

END
$$ LANGUAGE plpgsql;

-- upsert tuple while ensuring that the modified timestamp is >= than the current
-- timestamp if the record exists. if the timestamp parameter is null, the
-- tuple is inserted using a modified_at of NOW or updated leaving the existing
-- modified_at intact.
-- Return values:
--  'inserted' on insert
--  'updated' on successful update
--  'rejected' when the modified at does not satisfy the >= condition
CREATE OR REPLACE FUNCTION upsert_kv_data(b TEXT, k TEXT, d TEXT, m timestamptz DEFAULT NULL) RETURNS TEXT AS
$$
DECLARE
  _rows INTEGER;
BEGIN

    BEGIN
      IF (m IS NOT NULL) THEN
        EXECUTE format('INSERT INTO poky_%I (bucket, key, data, modified_at) VALUES (%L, %L, %L, %L)', b, b, k, d, m);
      ELSE
        EXECUTE format('INSERT INTO poky_%I (bucket, key, data) VALUES (%L, %L, %L)', b, b, k, d);
      END IF;

      RETURN 'inserted';
    EXCEPTION WHEN OTHERS THEN
      IF (m IS NOT NULL) THEN
        EXECUTE format('UPDATE poky_%I SET data = %L, modified_at = %L WHERE key = %L AND bucket = %L ', b, d, m, k, b);
      ELSE
        RAISE NOTICE 'inserting here';
        EXECUTE format('UPDATE poky_%I SET data = %L WHERE key = %L AND bucket = %L ', b, d, k, b);
      END IF;

      -- EXECUTE does not update FOUND
      GET DIAGNOSTICS _rows = ROW_COUNT;

      IF (_rows = 1 /* FOUND */) THEN
        RETURN 'updated';
      ELSE
        RETURN 'rejected';
      END IF;
    END;

END;
$$
LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION delete_kv_data(b TEXT, k TEXT) RETURNS INTEGER AS
$$
DECLARE
  _rows INTEGER;
BEGIN
  EXECUTE format('DELETE FROM poky_%I WHERE bucket = %L AND key = %L', b, b, k);
  GET DIAGNOSTICS _rows = ROW_COUNT;
  RETURN _rows;
END;
$$
LANGUAGE plpgsql;


-- delete all of the keys in a bucket.
-- warning, this will create a lot of cruft in the table if the bucket is large
CREATE OR REPLACE FUNCTION purge_bucket(b TEXT) RETURNS INTEGER AS
$$
DECLARE
  _rows INTEGER;
BEGIN
  EXECUTE format('DELETE FROM poky_%I', b);
  GET DIAGNOSTICS _rows = ROW_COUNT;
  RETURN _rows;
END;
$$
LANGUAGE plpgsql;

