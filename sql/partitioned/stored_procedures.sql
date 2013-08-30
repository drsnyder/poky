CREATE OR REPLACE FUNCTION create_bucket_partition(_bucket text) RETURNS VOID AS
$$
DECLARE
BEGIN

  EXECUTE format(
    'CREATE TABLE poky_%I (
      PRIMARY KEY (key),
      CHECK (bucket::text = %L)
    ) INHERITS (poky)',
    _bucket, _bucket);

  EXECUTE format('CREATE TRIGGER poky_%I_only_if_unmodified_since BEFORE UPDATE ON poky_%I
          FOR EACH ROW EXECUTE PROCEDURE only_if_unmodified_since()', _bucket, _bucket);

END
$$ LANGUAGE plpgsql;

