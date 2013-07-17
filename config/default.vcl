import std;
backend poky {
  .host = "127.0.0.1";
  .port = "9091";               # make sure this syncs up with the deploy config
  .connect_timeout = 1s;
  .first_byte_timeout = 5s;
  .between_bytes_timeout = 2s;
  .probe = {
      .url = "/status";
      .interval = 5s;
      .timeout = 1s;
      .window = 5;
      .threshold = 3;
  }
}

acl purgers {
    "127.0.0.1";
    "10.0.0.0"/8;
    "192.0.0.0"/8;
}


sub vcl_recv {
    # If-Match is stripped before being sent to the backend
    set req.http.X-If-Match = req.http.If-Match;
    set req.backend = poky;
    set req.grace = 5m;

    if (req.request == "PURGE") {
        if (!client.ip ~ purgers) {
            error 405 "Method not allowed";
        }

        return (lookup);
    }

    if (req.url ~ "^/status$") {
        return (pass);
    }
}

sub vcl_hash {
    # we only ever need to hash on the req.url since there is only one
    # host/server.
    hash_data(req.url);
    return (hash);
}


sub vcl_fetch {
    set beresp.http.x-url = req.url;
    set beresp.http.x-host = req.http.host;
    set beresp.grace = 5m;

    if (beresp.status == 200) {
        set beresp.ttl = 4h;
    }

    if (beresp.status == 404) {
        # we want to bypass the default hit_for_pass object so we can get HITs on this immediately
        # if it's updated. otherwise, we will encounter misses for 120s
        set beresp.ttl = 0s;
        return (deliver);
    }

    if (beresp.status >= 500) {
        set beresp.ttl = 0s;
    }
}

sub vcl_deliver {
    set resp.http.X-Served-By = server.identity;
    if (obj.hits > 0) {
        set resp.http.X-Cache = "HIT";
    } else {
        set resp.http.X-Cache = "MISS";
    }
}

sub vcl_hit {
        if (req.request == "PURGE") {
                purge;
                error 200 "Purged.";
        }
}

sub vcl_miss {
        if (req.request == "PURGE") {
                purge;
                error 200 "Purged.";
        }
}

sub vcl_pass {
        if (req.request == "PURGE") {
                error 503 "Shouldn't get to (pass) on PURGE.";
        }
}
