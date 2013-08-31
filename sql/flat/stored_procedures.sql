-- INSERT/UPDATE wrapper. Only call this function when inserting or deleting.
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
BEGIN
    BEGIN
        IF (m IS NOT NULL) THEN
            INSERT INTO poky (bucket, key, data, modified_at) VALUES (b, k, d, m);
        ELSE
            INSERT INTO poky (bucket, key, data) VALUES (b, k, d);
        END IF;

        RETURN 'inserted';
    EXCEPTION WHEN unique_violation THEN
        IF (m IS NOT NULL) THEN
            UPDATE poky SET data = d, modified_at = m WHERE key = k AND bucket = b;
        ELSE
            UPDATE poky SET data = d WHERE key = k AND bucket = b;
        END IF;

        IF (FOUND) THEN
            RETURN 'updated';
        ELSE
            RETURN 'rejected';
        END IF;
    END;
END;
$$
LANGUAGE plpgsql;


-- DELETE wrapper. Only call this function when deleting.
CREATE OR REPLACE FUNCTION delete_kv_data(b TEXT, k TEXT) RETURNS INTEGER AS
$$
DECLARE
  _rows INTEGER;
BEGIN
  DELETE FROM poky WHERE bucket = b AND key = k;
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
  DELETE FROM poky WHERE bucket = b;
  GET DIAGNOSTICS _rows = ROW_COUNT;
  RETURN _rows;
END;
$$
LANGUAGE plpgsql;
