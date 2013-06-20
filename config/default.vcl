backend poky {
  .host = "127.0.0.1";
  .port = "8081";               # make sure this syncs up with the deploy config
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
}

sub vcl_hash {

    # if the If-Match header is supplied ensure that the resource being
    # requested is differentiated by it's contents.
    if (req.http.X-If-Match) {
        hash_data(req.http.X-If-Match);
    }

}

sub vcl_recv {
    # If-Match is stripped
    set req.http.X-If-Match = req.http.If-Match;
    set req.backend = poky;
    set req.grace = 5m;

    # don't use this for hashing
    unset req.http.Host;

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

sub vcl_fetch {
    set beresp.grace = 5m;

    if (beresp.status == 200) {
        set beresp.ttl = 1h;
    }

    if (beresp.status == 404) {
        set beresp.ttl = 0s;
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
        error 200 "Purged: " + req.url;
    }
}

sub vcl_miss {
    if (req.request == "PURGE") {
        purge;
        error 404 "Not in the cache: " + req.url;
    }
}

sub vcl_pass {
    if (req.request == "PURGE") {
        error 502 "PURGE on a passed object";
    }
}
