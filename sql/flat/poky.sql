CREATE TABLE poky (
    bucket varchar(256) NOT NULL,
    key varchar(1024) NOT NULL,
    data text,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    modified_at timestamptz NOT NULL DEFAULT NOW(),
    PRIMARY KEY (bucket, key) /* USING INDEX TABLESPACE poky */
) /* TABLESPACE poky */;
