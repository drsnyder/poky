CREATE OR REPLACE FUNCTION poky_parent_trigger() RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION 'No % on poky directly. Use the bucket child table.', TG_OP;
END;
$$
LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS poky_parent ON poky;
CREATE TRIGGER poky_parent BEFORE INSERT OR UPDATE OR DELETE ON poky
        FOR EACH STATEMENT EXECUTE PROCEDURE poky_parent_trigger();

