CREATE OR REPLACE FUNCTION only_if_unmodified_since() RETURNS trigger AS $$
BEGIN
    IF NEW.modified_at >= OLD.modified_at THEN
        RETURN NEW;
    ELSE
        -- short circuit the update if the condition is not satisfied
        RAISE NOTICE 'Bypassing update. The current record is newer.';
        RETURN NULL;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION upsert_kv_data(b TEXT, k TEXT, d TEXT, m timestamptz) RETURNS VOID AS
$$
BEGIN
    -- from http://www.postgresql.org/docs/current/static/plpgsql-control-structures.html#PLPGSQL-UPSERT-EXAMPLE

    -- first try to update the key
    -- this won't work with the above only_if_unmodified_since() because found will be false since
    -- the query did not affect any rows (http://www.postgresql.org/docs/8.2/static/plpgsql-statements.html)
    -- if you add the surrounding loop, you end up in an infinite loop because the record is never found

    -- can we select for update into a row and if the row exists, try the update, OTW, insert?
    UPDATE poky SET data = d, modified_at = m WHERE key = k AND bucket = b;
    IF found THEN
        RETURN;
    END IF;
    -- not there, so try to insert the key
    -- if someone else inserts the same key concurrently,
    -- we could get a unique-key failure
    BEGIN
        INSERT INTO poky (bucket, key, data, modified_at) VALUES (b, k, d, m);
        RETURN;
    EXCEPTION WHEN unique_violation THEN
        -- Do nothing, and loop to try the UPDATE again.
    END;
END;
$$
LANGUAGE plpgsql;


CREATE TRIGGER poky_only_if_unmodified_since
 BEFORE UPDATE ON poky
   FOR EACH ROW EXECUTE PROCEDURE only_if_unmodified_since();
