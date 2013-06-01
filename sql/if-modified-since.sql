-- Ensure that we only accept updates to poky if the modified_at timestamp is
-- greater than or equal to the current modified_at.
CREATE OR REPLACE FUNCTION only_if_unmodified_since() RETURNS trigger AS $$
BEGIN
    IF NEW.modified_at >= OLD.modified_at THEN
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



-- upsert tuple while ensuring that the modified timestamp is >= than the current timestamp if the record exists.
-- Return values:
--  'inserted' on insert
--  'updated' on successful update
--  'rejected' when the modified at does not satisfy the >= condition
CREATE OR REPLACE FUNCTION upsert_kv_data(b TEXT, k TEXT, d TEXT, m timestamptz) RETURNS TEXT AS
$$
DECLARE
    existing_row_lock RECORD;
BEGIN
    BEGIN
        INSERT INTO poky (bucket, key, data, modified_at) VALUES (b, k, d, m);
        RETURN 'inserted';
    EXCEPTION WHEN unique_violation THEN
        UPDATE poky SET data = d, modified_at = m WHERE key = k AND bucket = b;
        IF (FOUND) THEN
            RETURN 'updated';
        ELSE
            RETURN 'rejected';
        END IF;
    END;
END;
$$
LANGUAGE plpgsql;
