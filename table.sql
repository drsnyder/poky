CREATE TABLE poky_text (
    key VARCHAR(1024) PRIMARY KEY, 
    data text
);

CREATE TABLE poky_json (
    key VARCHAR(1024) PRIMARY KEY, 
    data json
);

CREATE OR REPLACE FUNCTION get_json_key(structure JSON, key TEXT) RETURNS TEXT
AS $get_json_key$
  var js_object = structure;
  if (typeof js_object != 'object') {
     return typeof js_object;
  }
  return JSON.stringify(js_object[key]);
$get_json_key$
IMMUTABLE STRICT LANGUAGE plv8;

CREATE OR REPLACE FUNCTION merge_json_objects(a JSON, b JSON) RETURNS TEXT
AS $merge_json_objects$
  Object.extend = function(destination, source) {
      for (var property in source) {
          if (source.hasOwnProperty(property)) {
              destination[property] = source[property];
          }
      }
      return destination;
  };

  var obj_a = a;
  var obj_b = b;
  if (typeof obj_a != 'object') {
     return null;
  }

  if (typeof obj_b != 'object') {
     return null;
  }

  return JSON.stringify(Object.extend(obj_a, obj_b));
$merge_json_objects$
IMMUTABLE STRICT LANGUAGE plv8;
