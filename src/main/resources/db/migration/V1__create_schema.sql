create table if not exists stored_url (
    long_url    text                        primary key,
    hash        varchar(20)                 unique not null,
    created_at  timestamp with time zone    not null default now()
);
