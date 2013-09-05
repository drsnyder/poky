DO
$$
BEGIN

IF EXISTS (
    SELECT *
    FROM   pg_catalog.pg_tables
    WHERE  schemaname = 'public'
    AND    tablename  = 'poky'
    ) THEN
   RAISE NOTICE 'Table "public"."poky" already exists.';
ELSE
  CREATE TABLE poky (
      bucket varchar(256) NOT NULL,
      key varchar(1024) NOT NULL,
      data text,
      created_at timestamptz NOT NULL DEFAULT NOW() CONSTRAINT created_at_utc_check CHECK (EXTRACT(TIMEZONE FROM created_at) = '0'),
      modified_at timestamptz NOT NULL DEFAULT NOW() CONSTRAINT modified_at_utc_check CHECK (EXTRACT(TIMEZONE FROM modified_at) = '0'),
      PRIMARY KEY (bucket, key) /* USING INDEX TABLESPACE poky */
  ) /* TABLESPACE poky */;
END IF;

END;
$$ LANGUAGE plpgsql;
