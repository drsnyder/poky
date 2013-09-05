DROP TRIGGER IF EXISTS poky_only_if_unmodified_since ON poky;
CREATE TRIGGER poky_only_if_unmodified_since
 BEFORE UPDATE ON poky
   FOR EACH ROW EXECUTE PROCEDURE only_if_unmodified_since();

