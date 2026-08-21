begin;

create extension if not exists pgcrypto;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text not null default 'Poi member' check (char_length(display_name) between 1 and 60),
    handle text unique,
    home_area text not null default 'Mangaluru' check (char_length(home_area) <= 100),
    role text not null default 'member' check (role in ('member', 'admin')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create or replace function public.handle_new_poi_user()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    insert into public.profiles (id, display_name, handle)
    values (
        new.id,
        coalesce(nullif(new.raw_user_meta_data ->> 'display_name', ''), 'Poi member'),
        '@' || substr(replace(new.id::text, '-', ''), 1, 12)
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute procedure public.handle_new_poi_user();

revoke all on function public.handle_new_poi_user() from public;

create or replace function public.is_poi_admin()
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select exists (
        select 1 from public.profiles
        where id = auth.uid() and role = 'admin'
    );
$$;

revoke all on function public.is_poi_admin() from public;
grant execute on function public.is_poi_admin() to authenticated;

create table if not exists public.events (
    id uuid primary key default gen_random_uuid(),
    created_by uuid references public.profiles(id) on delete set null default auth.uid(),
    title text not null check (char_length(title) between 1 and 80),
    summary text not null check (char_length(summary) between 1 and 300),
    description text not null default '',
    category text not null check (category in ('festival','sale','concert','community','sports','workshop','private')),
    starts_at_millis bigint not null,
    ends_at_millis bigint not null check (ends_at_millis > starts_at_millis),
    venue text not null check (char_length(venue) between 1 and 100),
    address text not null check (char_length(address) between 1 and 160),
    latitude double precision,
    longitude double precision,
    distance_km double precision not null default 0 check (distance_km >= 0),
    organizer_name text not null check (char_length(organizer_name) between 1 and 100),
    organizer_verified boolean not null default false,
    visibility text not null default 'public' check (visibility in ('public','circle','invite_only')),
    verification text not null default 'community' check (verification in ('community','confirmed','organizer','official')),
    attendee_count integer not null default 0 check (attendee_count >= 0),
    friend_names jsonb not null default '[]'::jsonb,
    theme_key text not null default 'community',
    featured boolean not null default false,
    is_cancelled boolean not null default false,
    updated_at_millis bigint,
    created_at timestamptz not null default now()
);

create table if not exists public.attendance (
    user_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    event_id uuid not null references public.events(id) on delete cascade,
    status text not null check (status in ('interested','going','here','attended')),
    visibility text not null default 'friends' check (visibility in ('private','friends','attendees')),
    updated_at timestamptz not null default now(),
    primary key (user_id, event_id)
);

create table if not exists public.reports (
    id uuid primary key default gen_random_uuid(),
    reporter_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    event_id uuid not null references public.events(id) on delete cascade,
    reason text not null check (char_length(reason) between 1 and 500),
    reported_at_millis bigint not null,
    state text not null default 'open' check (state in ('open','reviewing','resolved','dismissed')),
    unique (reporter_id, event_id)
);

create or replace function public.protect_poi_event_fields()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    if old.created_by is distinct from new.created_by then
        raise exception 'created_by cannot be changed';
    end if;
    if not public.is_poi_admin() then
        new.organizer_verified := old.organizer_verified;
        new.verification := old.verification;
        new.featured := old.featured;
        new.attendee_count := old.attendee_count;
    end if;
    return new;
end;
$$;

drop trigger if exists protect_poi_event_fields on public.events;
create trigger protect_poi_event_fields
before update on public.events
for each row execute procedure public.protect_poi_event_fields();

revoke all on function public.protect_poi_event_fields() from public;

alter table public.profiles enable row level security;
alter table public.events enable row level security;
alter table public.attendance enable row level security;
alter table public.reports enable row level security;

create policy "profiles_read_own" on public.profiles
for select to authenticated using (id = auth.uid() or public.is_poi_admin());
create policy "profiles_update_own" on public.profiles
for update to authenticated using (id = auth.uid()) with check (id = auth.uid());

create policy "events_public_read" on public.events
for select to anon using (visibility = 'public');
create policy "events_member_read" on public.events
for select to authenticated using (
    visibility = 'public' or created_by = auth.uid() or public.is_poi_admin()
);
create policy "events_member_create" on public.events
for insert to authenticated with check (created_by = auth.uid());
create policy "events_owner_update" on public.events
for update to authenticated using (created_by = auth.uid() or public.is_poi_admin())
with check (created_by = auth.uid() or public.is_poi_admin());
create policy "events_owner_delete" on public.events
for delete to authenticated using (created_by = auth.uid() or public.is_poi_admin());

create policy "attendance_own_read" on public.attendance
for select to authenticated using (user_id = auth.uid() or public.is_poi_admin());
create policy "attendance_own_create" on public.attendance
for insert to authenticated with check (user_id = auth.uid());
create policy "attendance_own_update" on public.attendance
for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy "attendance_own_delete" on public.attendance
for delete to authenticated using (user_id = auth.uid());

create policy "reports_own_or_admin_read" on public.reports
for select to authenticated using (reporter_id = auth.uid() or public.is_poi_admin());
create policy "reports_create" on public.reports
for insert to authenticated with check (reporter_id = auth.uid());
create policy "reports_own_delete" on public.reports
for delete to authenticated using (reporter_id = auth.uid() or public.is_poi_admin());
create policy "reports_admin_update" on public.reports
for update to authenticated using (public.is_poi_admin()) with check (public.is_poi_admin());

revoke all on public.profiles, public.events, public.attendance, public.reports from anon, authenticated;
grant select on public.events to anon;
grant select, insert, update, delete on public.events to authenticated;
grant select on public.profiles to authenticated;
grant update (display_name, handle, home_area, updated_at) on public.profiles to authenticated;
grant select, insert, update, delete on public.attendance to authenticated;
grant select, insert, update, delete on public.reports to authenticated;

do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'events'
    ) then
        alter publication supabase_realtime add table public.events;
    end if;
end $$;

insert into public.events (
    id, title, summary, description, category, starts_at_millis, ends_at_millis,
    venue, address, distance_km, organizer_name, organizer_verified, visibility,
    verification, attendee_count, friend_names, theme_key, featured
) values
(
    '11111111-1111-4111-8111-111111111111',
    'Kudla Village Festival',
    'Food stalls, folk performances and a community procession.',
    'A celebration of coastal culture with local food, crafts, music and evening performances.',
    'festival',
    (extract(epoch from now()) * 1000)::bigint,
    (extract(epoch from now() + interval '7 hours') * 1000)::bigint,
    'Kadri Grounds', 'Kadri, Mangaluru', 2.4,
    'Kadri Community Council', true, 'public', 'official', 428,
    '["Ananya","Rohan","Meera"]'::jsonb, 'festival', true
),
(
    '22222222-2222-4222-8222-222222222222',
    'Coastal Music Under the Stars',
    'An open-air evening with indie and folk artists.',
    'Bring a mat and enjoy performances from independent musicians across coastal Karnataka.',
    'concert',
    (extract(epoch from now() + interval '1 day') * 1000)::bigint,
    (extract(epoch from now() + interval '1 day 5 hours') * 1000)::bigint,
    'Tannirbhavi Beach', 'Tannirbhavi, Mangaluru', 7.8,
    'Coastline Collective', true, 'public', 'organizer', 216,
    '["Arjun","Nisha"]'::jsonb, 'concert', false
),
(
    '33333333-3333-4333-8333-333333333333',
    'Monsoon Street Sale',
    'Local shops, handmade products and seasonal offers.',
    'A neighbourhood shopping street featuring verified local sellers and family activities.',
    'sale',
    (extract(epoch from now() + interval '2 days') * 1000)::bigint,
    (extract(epoch from now() + interval '2 days 9 hours') * 1000)::bigint,
    'Car Street', 'Hampankatta, Mangaluru', 1.3,
    'Local Traders Association', true, 'public', 'confirmed', 94,
    '["Meera"]'::jsonb, 'sale', false
)
on conflict (id) do nothing;

commit;
