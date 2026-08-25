begin;

-- Private and circle events remain invisible unless the owner explicitly grants access.
create table if not exists public.event_members (
    event_id uuid not null references public.events(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    invited_by uuid references public.profiles(id) on delete set null default auth.uid(),
    created_at timestamptz not null default now(),
    primary key (event_id, user_id)
);

create or replace function public.can_manage_poi_event(target_event_id uuid)
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select public.is_poi_admin() or exists (
        select 1 from public.events event
        where event.id = target_event_id and event.created_by = auth.uid()
    );
$$;

create or replace function public.is_poi_event_member(target_event_id uuid)
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select exists (
        select 1 from public.event_members member
        where member.event_id = target_event_id and member.user_id = auth.uid()
    );
$$;

revoke all on function public.can_manage_poi_event(uuid) from public;
revoke all on function public.is_poi_event_member(uuid) from public;
grant execute on function public.can_manage_poi_event(uuid) to authenticated;
grant execute on function public.is_poi_event_member(uuid) to authenticated;

create or replace function public.can_read_poi_event(target_event_id uuid)
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select exists (
        select 1 from public.events event
        where event.id = target_event_id
          and (
              event.visibility = 'public'
              or event.created_by = auth.uid()
              or public.is_poi_event_member(event.id)
              or public.is_poi_admin()
          )
    );
$$;

revoke all on function public.can_read_poi_event(uuid) from public;
grant execute on function public.can_read_poi_event(uuid) to anon, authenticated;

alter table public.event_members enable row level security;

drop policy if exists "events_member_read" on public.events;
create policy "events_member_read" on public.events
for select to authenticated using (
    visibility = 'public'
    or created_by = auth.uid()
    or public.is_poi_event_member(id)
    or public.is_poi_admin()
);

drop policy if exists "event_members_allowed_read" on public.event_members;
create policy "event_members_allowed_read" on public.event_members
for select to authenticated using (
    user_id = auth.uid() or public.can_manage_poi_event(event_id)
);
drop policy if exists "event_members_owner_create" on public.event_members;
create policy "event_members_owner_create" on public.event_members
for insert to authenticated with check (
    public.can_manage_poi_event(event_id) and invited_by = auth.uid()
);
drop policy if exists "event_members_owner_or_self_delete" on public.event_members;
create policy "event_members_owner_or_self_delete" on public.event_members
for delete to authenticated using (
    user_id = auth.uid() or public.can_manage_poi_event(event_id)
);

revoke all on public.event_members from anon, authenticated;
grant select, insert, delete on public.event_members to authenticated;

drop policy if exists "attendance_own_create" on public.attendance;
create policy "attendance_own_create" on public.attendance
for insert to authenticated with check (
    user_id = auth.uid() and public.can_read_poi_event(event_id)
);
drop policy if exists "attendance_own_update" on public.attendance;
create policy "attendance_own_update" on public.attendance
for update to authenticated using (user_id = auth.uid()) with check (
    user_id = auth.uid() and public.can_read_poi_event(event_id)
);

drop policy if exists "reports_create" on public.reports;
create policy "reports_create" on public.reports
for insert to authenticated with check (
    reporter_id = auth.uid() and public.can_read_poi_event(event_id)
);

-- Client event updates cannot forge trust or attendance fields. The attendance
-- counter is allowed only while invoked from its nested database trigger.
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
        if pg_trigger_depth() <= 1 then
            new.attendee_count := old.attendee_count;
        end if;
    end if;
    return new;
end;
$$;

revoke all on function public.protect_poi_event_fields() from public;

-- Keep attendee counts authoritative on the server instead of trusting clients.
create or replace function public.sync_poi_event_attendee_count()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare target_event_id uuid;
declare previous_event_id uuid;
begin
    if tg_op = 'DELETE' then
        target_event_id := old.event_id;
    else
        target_event_id := new.event_id;
        if tg_op = 'UPDATE' and old.event_id is distinct from new.event_id then
            previous_event_id := old.event_id;
        end if;
    end if;

    update public.events event
    set attendee_count = (
        select count(*)::integer
        from public.attendance attendance
        where attendance.event_id = target_event_id
          and attendance.status in ('going', 'here', 'attended')
    ), updated_at_millis = (extract(epoch from now()) * 1000)::bigint
    where event.id = target_event_id;

    if previous_event_id is not null then
        update public.events event
        set attendee_count = (
            select count(*)::integer
            from public.attendance attendance
            where attendance.event_id = previous_event_id
              and attendance.status in ('going', 'here', 'attended')
        ), updated_at_millis = (extract(epoch from now()) * 1000)::bigint
        where event.id = previous_event_id;
    end if;

    if tg_op = 'DELETE' then return old; else return new; end if;
end;
$$;

revoke all on function public.sync_poi_event_attendee_count() from public;
drop trigger if exists sync_poi_event_attendee_count on public.attendance;
create trigger sync_poi_event_attendee_count
after insert or update or delete on public.attendance
for each row execute procedure public.sync_poi_event_attendee_count();

-- Keep profile timestamps server-owned and consistent across clients.
create or replace function public.touch_poi_profile_updated_at()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

revoke all on function public.touch_poi_profile_updated_at() from public;
drop trigger if exists touch_poi_profile_updated_at on public.profiles;
create trigger touch_poi_profile_updated_at
before update on public.profiles
for each row execute procedure public.touch_poi_profile_updated_at();

-- Replace the first moments migration's record coalescing with DELETE-safe triggers.
create or replace function public.sync_poi_moment_like_count()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare target_moment_id uuid;
begin
    if tg_op = 'DELETE' then target_moment_id := old.moment_id;
    else target_moment_id := new.moment_id;
    end if;
    update public.event_moments moment
    set like_count = (
        select count(*)::integer from public.moment_likes likes
        where likes.moment_id = target_moment_id
    )
    where moment.id = target_moment_id;
    if tg_op = 'DELETE' then return old; else return new; end if;
end;
$$;

create or replace function public.sync_poi_moment_comment_count()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare target_moment_id uuid;
begin
    if tg_op = 'DELETE' then target_moment_id := old.moment_id;
    else target_moment_id := new.moment_id;
    end if;
    update public.event_moments moment
    set comment_count = (
        select count(*)::integer from public.moment_comments comments
        where comments.moment_id = target_moment_id
    )
    where moment.id = target_moment_id;
    if tg_op = 'DELETE' then return old; else return new; end if;
end;
$$;

-- Admin-only immutable audit history for privileged and moderation actions.
create table if not exists public.audit_log (
    id bigint generated always as identity primary key,
    actor_id uuid,
    action text not null check (char_length(action) between 1 and 80),
    entity_type text not null check (char_length(entity_type) between 1 and 40),
    entity_id uuid,
    details jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

alter table public.audit_log enable row level security;
drop policy if exists "audit_log_admin_read" on public.audit_log;
create policy "audit_log_admin_read" on public.audit_log
for select to authenticated using (public.is_poi_admin());
revoke all on public.audit_log from anon, authenticated;
grant select on public.audit_log to authenticated;

create or replace function public.audit_poi_event_change()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare row_id uuid;
declare row_title text;
declare row_visibility text;
begin
    if tg_op = 'DELETE' then
        row_id := old.id;
        row_title := old.title;
        row_visibility := old.visibility;
    else
        row_id := new.id;
        row_title := new.title;
        row_visibility := new.visibility;
    end if;
    insert into public.audit_log (actor_id, action, entity_type, entity_id, details)
    values (
        auth.uid(),
        'event_' || lower(tg_op),
        'event',
        row_id,
        jsonb_build_object('title', row_title, 'visibility', row_visibility)
    );
    if tg_op = 'DELETE' then return old; else return new; end if;
end;
$$;

create or replace function public.audit_poi_report_change()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare row_id uuid;
declare target_event_id uuid;
declare row_state text;
begin
    if tg_op = 'DELETE' then
        row_id := old.id;
        target_event_id := old.event_id;
        row_state := old.state;
    else
        row_id := new.id;
        target_event_id := new.event_id;
        row_state := new.state;
    end if;
    insert into public.audit_log (actor_id, action, entity_type, entity_id, details)
    values (
        auth.uid(),
        'report_' || lower(tg_op),
        'report',
        row_id,
        jsonb_build_object('event_id', target_event_id, 'state', row_state)
    );
    if tg_op = 'DELETE' then return old; else return new; end if;
end;
$$;

revoke all on function public.audit_poi_event_change() from public;
revoke all on function public.audit_poi_report_change() from public;
drop trigger if exists audit_poi_event_change on public.events;
create trigger audit_poi_event_change
after insert or update or delete on public.events
for each row execute procedure public.audit_poi_event_change();
drop trigger if exists audit_poi_report_change on public.reports;
create trigger audit_poi_report_change
after insert or update or delete on public.reports
for each row execute procedure public.audit_poi_report_change();

-- Account deletion is initiated only by the authenticated account itself.
create or replace function public.delete_poi_account()
returns void
language plpgsql
security definer set search_path = ''
as $$
declare caller_id uuid := auth.uid();
begin
    if caller_id is null then
        raise exception 'Authentication is required';
    end if;

    insert into public.audit_log (actor_id, action, entity_type, entity_id)
    values (caller_id, 'account_delete', 'profile', caller_id);

    -- Public listings survive as community records; private listings do not.
    delete from public.events
    where created_by = caller_id and visibility <> 'public';

    delete from auth.users where id = caller_id;
end;
$$;

revoke all on function public.delete_poi_account() from public;
grant execute on function public.delete_poi_account() to authenticated;

-- Realtime keeps event, attendance, moderation, and profile state consistent across devices.
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'attendance'
    ) then
        alter publication supabase_realtime add table public.attendance;
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'reports'
    ) then
        alter publication supabase_realtime add table public.reports;
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'profiles'
    ) then
        alter publication supabase_realtime add table public.profiles;
    end if;
end $$;

commit;
