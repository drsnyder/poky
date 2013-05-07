CREATE TABLE poky (
    bucket varchar(256) NOT NULL,
    key varchar(1024) NOT NULL,
    data text,
    created_at timestamptz DEFAULT NOW(),
    modified_at timestamptz DEFAULT NOW(),
    PRIMARY KEY (bucket, key)
);
