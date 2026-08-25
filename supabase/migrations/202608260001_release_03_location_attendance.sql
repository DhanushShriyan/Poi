begin;

alter table public.events
    add column if not exists check_in_radius_meters integer not null default 500;

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'events_check_in_radius_meters_check'
    ) then
        alter table public.events add constraint events_check_in_radius_meters_check
            check (check_in_radius_meters between 100 and 5000);
    end if;
end $$;

alter table public.attendance
    add column if not exists check_in_latitude double precision,
    add column if not exists check_in_longitude double precision,
    add column if not exists accuracy_meters integer,
    add column if not exists verification_method text not null default 'manual',
    add column if not exists distance_meters integer,
    add column if not exists verified_at_millis bigint;

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'attendance_verification_method_check'
    ) then
        alter table public.attendance add constraint attendance_verification_method_check
            check (verification_method in ('manual', 'proximity'));
    end if;
    if not exists (
        select 1 from pg_constraint where conname = 'attendance_accuracy_meters_check'
    ) then
        alter table public.attendance add constraint attendance_accuracy_meters_check
            check (accuracy_meters is null or accuracy_meters between 0 and 10000);
    end if;
    if not exists (
        select 1 from pg_constraint where conname = 'attendance_distance_meters_check'
    ) then
        alter table public.attendance add constraint attendance_distance_meters_check
            check (distance_meters is null or distance_meters >= 0);
    end if;
end $$;

create or replace function public.poi_distance_meters(
    latitude_one double precision,
    longitude_one double precision,
    latitude_two double precision,
    longitude_two double precision
)
returns double precision
language sql
immutable
strict
security invoker
set search_path = ''
as $$
    select 6371000.0 * 2.0 * asin(
        sqrt(
            least(
                1.0,
                greatest(
                    0.0,
                    power(sin(radians(latitude_two - latitude_one) / 2.0), 2) +
                    cos(radians(latitude_one)) * cos(radians(latitude_two)) *
                    power(sin(radians(longitude_two - longitude_one) / 2.0), 2)
                )
            )
        )
    );
$$;

revoke all on function public.poi_distance_meters(
    double precision, double precision, double precision, double precision
) from public;

-- Raw device coordinates exist only as transient trigger inputs. The database
-- retains the distance and accuracy result, then clears the coordinates.
create or replace function public.verify_poi_proximity_check_in()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare event_latitude double precision;
declare event_longitude double precision;
declare event_radius integer;
declare event_starts bigint;
declare event_ends bigint;
declare calculated_distance double precision;
declare current_millis bigint := (extract(epoch from now()) * 1000)::bigint;
begin
    if new.status = 'here' then
        select event.latitude, event.longitude, event.check_in_radius_meters,
               event.starts_at_millis, event.ends_at_millis
        into event_latitude, event_longitude, event_radius, event_starts, event_ends
        from public.events event
        where event.id = new.event_id;

        if current_millis < event_starts - 21600000 or current_millis > event_ends + 43200000 then
            raise exception 'Check-in is available only near the event time';
        end if;

        if new.check_in_latitude is not null and new.check_in_longitude is not null
           and event_latitude is not null and event_longitude is not null then
            if new.check_in_latitude not between -90 and 90
               or new.check_in_longitude not between -180 and 180 then
                raise exception 'Invalid check-in location';
            end if;

            calculated_distance := public.poi_distance_meters(
                event_latitude,
                event_longitude,
                new.check_in_latitude,
                new.check_in_longitude
            );

            if calculated_distance > event_radius + least(coalesce(new.accuracy_meters, 0), 200) then
                raise exception 'You appear to be outside this event check-in area';
            end if;

            new.verification_method := 'proximity';
            new.distance_meters := round(calculated_distance)::integer;
            new.accuracy_meters := greatest(coalesce(new.accuracy_meters, 0), 0);
            new.verified_at_millis := current_millis;
        else
            new.verification_method := 'manual';
            new.distance_meters := null;
            new.accuracy_meters := null;
            new.verified_at_millis := current_millis;
        end if;
    elsif new.status = 'attended' and tg_op = 'UPDATE'
          and old.verification_method = 'proximity' then
        new.verification_method := old.verification_method;
        new.distance_meters := old.distance_meters;
        new.accuracy_meters := old.accuracy_meters;
        new.verified_at_millis := old.verified_at_millis;
    else
        new.verification_method := 'manual';
        new.distance_meters := null;
        new.accuracy_meters := null;
        new.verified_at_millis := null;
    end if;

    new.check_in_latitude := null;
    new.check_in_longitude := null;
    return new;
end;
$$;

revoke all on function public.verify_poi_proximity_check_in() from public;
drop trigger if exists verify_poi_proximity_check_in on public.attendance;
create trigger verify_poi_proximity_check_in
before insert or update on public.attendance
for each row execute procedure public.verify_poi_proximity_check_in();

-- Starter listings receive approximate venue points for connected testing.
update public.events set latitude = 12.8867, longitude = 74.8556,
    check_in_radius_meters = 1200
where id = '11111111-1111-4111-8111-111111111111';
update public.events set latitude = 12.9266, longitude = 74.8137,
    check_in_radius_meters = 1500
where id = '22222222-2222-4222-8222-222222222222';
update public.events set latitude = 12.8718, longitude = 74.8387,
    check_in_radius_meters = 600
where id = '33333333-3333-4333-8333-333333333333';

commit;
