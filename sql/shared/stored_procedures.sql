-- Examples:
-- SELECT * FROM mget('bucket', ARRAY[('key', '2013-05-18 16:43:23.361331+00')::mget_param_row]);
-- SELECT * FROM mget('bucket', ARRAY[('key', NULL)::mget_param_row]);

DO
  $$
  BEGIN
  IF (SELECT 1 FROM pg_type WHERE typname = 'mget_param_row') THEN
    RAISE NOTICE 'Type mget_param_row exists. Skipping.';
  ELSE
    CREATE TYPE mget_param_row AS ( key text, modified_at timestamptz );
  END IF;
END $$ LANGUAGE PLPGSQL;


CREATE OR REPLACE FUNCTION mget(_bucket text, _values mget_param_row[]) RETURNS TABLE
(
  bucket varchar(256),
  key    varchar(1024),
  data   text,
  created_at timestamptz,
  modified_at timestamptz
) AS
$$
  WITH mget_poky AS (
    SELECT * FROM UNNEST(_values)
  ),
  poky AS (
    SELECT *
    FROM poky
    WHERE bucket = _bucket AND
    key IN (SELECT key FROM mget_poky)
  )
  SELECT poky.bucket, poky.key, poky.data, poky.created_at, poky.modified_at
  FROM poky
  JOIN mget_poky ON (
    mget_poky.key = poky.key AND
    (( mget_poky.modified_at IS NULL) OR date_trunc('seconds', poky.modified_at) = date_trunc('seconds', mget_poky.modified_at) )
  );
$$ LANGUAGE SQL STABLE CALLED ON NULL INPUT;


/***************************************************************************
FIXME partitioning: move upsert to flat & partition. The partition versions need to be table specific.
We also need to route deletes through a stored procedure.
***************************************************************************/

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

