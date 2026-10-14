create table meeting_types (
    id uuid primary key,
    name varchar(255) not null,
    description varchar(1000) not null,
    duration_minutes integer not null
);
