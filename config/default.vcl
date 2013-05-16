# TODO:
# - add probe for backend health
backend poky {
  .host = "127.0.0.1";
  .port = "8080";
  .connect_timeout = 1s;
  .first_byte_timeout = 5s;
  .between_bytes_timeout = 2s;
}

sub vcl_recv {
    set req.backend = poky;
    set req.grace = 5m;

    # on PUT, POST, or DELETE, we want to invalidate the given object
    if (req.request == "POST" || req.request == "PUT" || req.request == "DELETE") {
        ban("req.url ~ " + req.url);
    }
}

sub vcl_fetch {
    set beresp.grace = 5m;
    if (beresp.status == 200) {
        set beresp.ttl = 1h;
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
