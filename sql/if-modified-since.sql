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

CREATE TRIGGER poky_only_if_unmodified_since
 BEFORE UPDATE ON poky
   FOR EACH ROW EXECUTE PROCEDURE only_if_unmodified_since();
