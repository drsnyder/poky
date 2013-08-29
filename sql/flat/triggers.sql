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

-- alterations if the constraints don't exist
ALTER TABLE poky ALTER COLUMN modified_at SET NOT NULL;
ALTER TABLE poky ALTER COLUMN modified_at SET DEFAULT NOW();
ALTER TABLE poky ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE poky ALTER COLUMN created_at SET DEFAULT NOW();

-- ensure that we are using UTC
ALTER TABLE poky DROP CONSTRAINT IF EXISTS modified_at_utc_check;
ALTER TABLE poky ADD CONSTRAINT modified_at_utc_check CHECK (EXTRACT(TIMEZONE FROM modified_at) = '0');
ALTER TABLE poky DROP CONSTRAINT IF EXISTS created_at_utc_check;
ALTER TABLE poky ADD CONSTRAINT created_at_utc_check CHECK (EXTRACT(TIMEZONE FROM created_at) = '0');
