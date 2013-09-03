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
