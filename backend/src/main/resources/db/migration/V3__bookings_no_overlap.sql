alter table bookings
    add constraint bookings_no_overlap
    exclude using gist (tstzrange(start_at, end_at, '[)') with &&);
