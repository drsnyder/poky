CREATE OR REPLACE FUNCTION update_modified_at() RETURNS "trigger"
AS $$
BEGIN
    NEW.modified_at = NOW();
    RETURN NEW;
END;
$$LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS poky_modified_at ON poky;
CREATE TRIGGER poky_modified_at BEFORE UPDATE ON poky FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

