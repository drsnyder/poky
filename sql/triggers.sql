-- Ensure that we only accept updates to poky if the modified_at timestamp is
-- greater than or equal to the current modified_at.
CREATE OR REPLACE FUNCTION only_if_unmodified_since() RETURNS trigger AS $$
BEGIN
    IF (NEW.modified_at >= OLD.modified_at) THEN
        RETURN NEW;
    ELSE
        -- short circuit the update if the condition is not satisfied
        RETURN NULL;
    END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS poky_only_if_unmodified_since ON poky;
CREATE TRIGGER poky_only_if_unmodified_since
 BEFORE UPDATE ON poky
   FOR EACH ROW EXECUTE PROCEDURE only_if_unmodified_since();

ALTER TABLE poky ALTER COLUMN modified_at SET NOT NULL;
ALTER TABLE poky ALTER COLUMN modified_at SET DEFAULT NOW();
ALTER TABLE poky ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE poky ALTER COLUMN created_at SET DEFAULT NOW();

-- ensure that we are using UTC
ALTER TABLE poky DROP CONSTRAINT IF EXISTS modified_at_utc_check;
ALTER TABLE poky ADD CONSTRAINT modified_at_utc_check CHECK (EXTRACT(TIMEZONE FROM modified_at) = '0');
ALTER TABLE poky DROP CONSTRAINT IF EXISTS created_at_utc_check;
ALTER TABLE poky ADD CONSTRAINT created_at_utc_check CHECK (EXTRACT(TIMEZONE FROM created_at) = '0');


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
