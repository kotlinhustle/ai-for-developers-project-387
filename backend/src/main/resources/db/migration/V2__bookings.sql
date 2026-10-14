create table bookings (
    id uuid primary key,
    meeting_type_id uuid not null references meeting_types (id),
    start_at timestamptz not null,
    end_at timestamptz not null,
    guest_name varchar(255) not null,
    guest_email varchar(255) not null,
    comment varchar(1000)
);
