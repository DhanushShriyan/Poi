-- Release 0.4: limited public profiles, friendships, invitations and privacy-aware activity.

alter table public.profiles
    add column if not exists share_plans_to_friends boolean not null default true,
    add column if not exists share_friend_activity boolean not null default true;

create table if not exists public.public_profile_directory (
    id uuid primary key references public.profiles(id) on delete cascade,
    display_name text not null check (char_length(display_name) between 1 and 60),
    handle text not null check (char_length(handle) between 2 and 25),
    home_area text not null default 'Your area' check (char_length(home_area) between 1 and 100),
    updated_at_millis bigint not null
);

create or replace function public.sync_poi_public_profile()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    if new.role = 'admin' then
        delete from public.public_profile_directory where id = new.id;
        return new;
    end if;
    insert into public.public_profile_directory (
        id,
        display_name,
        handle,
        home_area,
        updated_at_millis
    ) values (
        new.id,
        new.display_name,
        coalesce(nullif(new.handle, ''), '@' || left(replace(new.id::text, '-', ''), 12)),
        coalesce(nullif(new.home_area, ''), 'Your area'),
        floor(extract(epoch from now()) * 1000)::bigint
    )
    on conflict (id) do update set
        display_name = excluded.display_name,
        handle = excluded.handle,
        home_area = excluded.home_area,
        updated_at_millis = excluded.updated_at_millis;
    return new;
end;
$$;

revoke all on function public.sync_poi_public_profile() from public;

drop trigger if exists sync_poi_public_profile on public.profiles;
create trigger sync_poi_public_profile
after insert or update of display_name, handle, home_area, role on public.profiles
for each row execute procedure public.sync_poi_public_profile();

insert into public.public_profile_directory (id, display_name, handle, home_area, updated_at_millis)
select
    profile.id,
    profile.display_name,
    coalesce(nullif(profile.handle, ''), '@' || left(replace(profile.id::text, '-', ''), 12)),
    coalesce(nullif(profile.home_area, ''), 'Your area'),
    floor(extract(epoch from now()) * 1000)::bigint
from public.profiles profile
where profile.role <> 'admin'
on conflict (id) do update set
    display_name = excluded.display_name,
    handle = excluded.handle,
    home_area = excluded.home_area,
    updated_at_millis = excluded.updated_at_millis;

create table if not exists public.friendships (
    id uuid primary key default gen_random_uuid(),
    requester_id uuid not null references public.profiles(id) on delete cascade,
    addressee_id uuid not null references public.profiles(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted')),
    created_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    updated_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    check (requester_id <> addressee_id)
);

create unique index if not exists friendships_unique_pair_idx
on public.friendships ((least(requester_id, addressee_id)), (greatest(requester_id, addressee_id)));
create index if not exists friendships_requester_idx on public.friendships(requester_id);
create index if not exists friendships_addressee_idx on public.friendships(addressee_id);

create or replace function public.protect_poi_friendship()
returns trigger language plpgsql security invoker set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'Sign in to manage friendships.';
    end if;

    if tg_op = 'INSERT' then
        if new.requester_id <> auth.uid() or new.requester_id = new.addressee_id then
            raise exception 'Invalid friend request.';
        end if;
        new.status := 'pending';
        new.created_at_millis := floor(extract(epoch from now()) * 1000)::bigint;
        new.updated_at_millis := new.created_at_millis;
        return new;
    end if;

    if new.requester_id <> old.requester_id or new.addressee_id <> old.addressee_id then
        raise exception 'Friendship participants cannot be changed.';
    end if;
    if old.status <> 'pending' or auth.uid() <> old.addressee_id or new.status <> 'accepted' then
        raise exception 'Only the recipient can accept a pending request.';
    end if;
    new.updated_at_millis := floor(extract(epoch from now()) * 1000)::bigint;
    return new;
end;
$$;

revoke all on function public.protect_poi_friendship() from public;
drop trigger if exists protect_poi_friendship on public.friendships;
create trigger protect_poi_friendship
before insert or update on public.friendships
for each row execute procedure public.protect_poi_friendship();

create or replace function public.are_poi_friends(first_user uuid, second_user uuid)
returns boolean language sql stable security definer set search_path = ''
as $$
    select exists (
        select 1
        from public.friendships friendship
        where friendship.status = 'accepted'
          and (
              (friendship.requester_id = first_user and friendship.addressee_id = second_user)
              or
              (friendship.requester_id = second_user and friendship.addressee_id = first_user)
          )
    );
$$;

revoke all on function public.are_poi_friends(uuid, uuid) from public;
grant execute on function public.are_poi_friends(uuid, uuid) to authenticated;

create or replace function public.can_share_poi_activity(profile_id uuid, requested_type text)
returns boolean language sql stable security definer set search_path = ''
as $$
    select coalesce((
        select case
            when requested_type in ('interested', 'going') then profile.share_plans_to_friends
            else profile.share_friend_activity
        end
        from public.profiles profile
        where profile.id = profile_id
    ), false);
$$;

revoke all on function public.can_share_poi_activity(uuid, text) from public;
grant execute on function public.can_share_poi_activity(uuid, text) to authenticated;

create table if not exists public.event_invitations (
    id uuid primary key default gen_random_uuid(),
    event_id uuid not null references public.events(id) on delete cascade,
    event_title text not null default 'Event invitation' check (char_length(event_title) between 1 and 80),
    inviter_id uuid not null references public.profiles(id) on delete cascade,
    invitee_id uuid not null references public.profiles(id) on delete cascade,
    status text not null default 'pending' check (status in ('pending', 'accepted')),
    created_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    updated_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    unique (event_id, invitee_id),
    check (inviter_id <> invitee_id)
);

create index if not exists event_invitations_invitee_idx on public.event_invitations(invitee_id);
create index if not exists event_invitations_inviter_idx on public.event_invitations(inviter_id);

create or replace function public.protect_poi_event_invitation()
returns trigger language plpgsql security definer set search_path = ''
as $$
declare
    event_record public.events%rowtype;
begin
    if auth.uid() is null then
        raise exception 'Sign in to manage invitations.';
    end if;

    if tg_op = 'INSERT' then
        if new.inviter_id <> auth.uid() or new.inviter_id = new.invitee_id then
            raise exception 'Invalid event invitation.';
        end if;
        if not public.are_poi_friends(new.inviter_id, new.invitee_id) then
            raise exception 'Only accepted friends can be invited.';
        end if;
        select * into event_record from public.events where id = new.event_id;
        if event_record.id is null then
            raise exception 'Event not found.';
        end if;
        if event_record.visibility <> 'public' and event_record.created_by <> auth.uid() then
            raise exception 'Only the event owner can invite people to a private event.';
        end if;
        new.event_title := event_record.title;
        new.status := 'pending';
        new.created_at_millis := floor(extract(epoch from now()) * 1000)::bigint;
        new.updated_at_millis := new.created_at_millis;
        return new;
    end if;

    if new.event_id <> old.event_id or new.inviter_id <> old.inviter_id or
       new.invitee_id <> old.invitee_id or new.event_title <> old.event_title then
        raise exception 'Invitation details cannot be changed.';
    end if;
    if auth.uid() <> old.invitee_id or old.status <> 'pending' or new.status <> 'accepted' then
        raise exception 'Only the invited person can accept this invitation.';
    end if;
    new.updated_at_millis := floor(extract(epoch from now()) * 1000)::bigint;
    return new;
end;
$$;

revoke all on function public.protect_poi_event_invitation() from public;
drop trigger if exists protect_poi_event_invitation on public.event_invitations;
create trigger protect_poi_event_invitation
before insert or update on public.event_invitations
for each row execute procedure public.protect_poi_event_invitation();

create or replace function public.accept_poi_event_invitation()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    if new.status = 'accepted' and old.status = 'pending' then
        insert into public.event_members (event_id, user_id, invited_by)
        values (new.event_id, new.invitee_id, new.inviter_id)
        on conflict (event_id, user_id) do nothing;
    end if;
    return new;
end;
$$;

revoke all on function public.accept_poi_event_invitation() from public;
drop trigger if exists accept_poi_event_invitation on public.event_invitations;
create trigger accept_poi_event_invitation
after update of status on public.event_invitations
for each row execute procedure public.accept_poi_event_invitation();

create table if not exists public.social_activity (
    id uuid primary key default gen_random_uuid(),
    actor_id uuid not null references public.profiles(id) on delete cascade,
    event_id uuid not null references public.events(id) on delete cascade,
    activity_type text not null check (activity_type in ('interested', 'going', 'here', 'hosting')),
    created_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    unique (actor_id, event_id, activity_type)
);

create index if not exists social_activity_actor_idx on public.social_activity(actor_id);
create index if not exists social_activity_event_idx on public.social_activity(event_id);
create index if not exists social_activity_created_idx on public.social_activity(created_at_millis desc);

create or replace function public.sync_poi_attendance_activity()
returns trigger language plpgsql security definer set search_path = ''
as $$
declare
    plans_enabled boolean;
    activity_enabled boolean;
begin
    if tg_op = 'DELETE' then
        delete from public.social_activity
        where actor_id = old.user_id and event_id = old.event_id and activity_type <> 'hosting';
        return old;
    end if;

    select profile.share_plans_to_friends, profile.share_friend_activity
    into plans_enabled, activity_enabled
    from public.profiles profile
    where profile.id = new.user_id;

    delete from public.social_activity
    where actor_id = new.user_id and event_id = new.event_id and activity_type <> 'hosting';

    if (new.status in ('interested', 'going') and plans_enabled)
       or (new.status = 'here' and activity_enabled) then
        insert into public.social_activity (actor_id, event_id, activity_type, created_at_millis)
        values (
            new.user_id,
            new.event_id,
            new.status,
            floor(extract(epoch from now()) * 1000)::bigint
        )
        on conflict (actor_id, event_id, activity_type) do update set
            created_at_millis = excluded.created_at_millis;
    end if;
    return new;
end;
$$;

revoke all on function public.sync_poi_attendance_activity() from public;
drop trigger if exists sync_poi_attendance_activity on public.attendance;
create trigger sync_poi_attendance_activity
after insert or update of status or delete on public.attendance
for each row execute procedure public.sync_poi_attendance_activity();

create or replace function public.sync_poi_hosting_activity()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    if new.created_by is not null and exists (
        select 1 from public.profiles profile
        where profile.id = new.created_by and profile.share_friend_activity
    ) then
        insert into public.social_activity (actor_id, event_id, activity_type, created_at_millis)
        values (
            new.created_by,
            new.id,
            'hosting',
            floor(extract(epoch from now()) * 1000)::bigint
        )
        on conflict (actor_id, event_id, activity_type) do update set
            created_at_millis = excluded.created_at_millis;
    end if;
    return new;
end;
$$;

revoke all on function public.sync_poi_hosting_activity() from public;
drop trigger if exists sync_poi_hosting_activity on public.events;
create trigger sync_poi_hosting_activity
after insert on public.events
for each row execute procedure public.sync_poi_hosting_activity();

insert into public.social_activity (actor_id, event_id, activity_type, created_at_millis)
select
    attendance.user_id,
    attendance.event_id,
    attendance.status,
    floor(extract(epoch from now()) * 1000)::bigint
from public.attendance attendance
join public.profiles profile on profile.id = attendance.user_id
where (attendance.status in ('interested', 'going') and profile.share_plans_to_friends)
   or (attendance.status = 'here' and profile.share_friend_activity)
on conflict (actor_id, event_id, activity_type) do update set
    created_at_millis = excluded.created_at_millis;

insert into public.social_activity (actor_id, event_id, activity_type, created_at_millis)
select
    event.created_by,
    event.id,
    'hosting',
    floor(extract(epoch from now()) * 1000)::bigint
from public.events event
join public.profiles profile on profile.id = event.created_by
where profile.share_friend_activity
on conflict (actor_id, event_id, activity_type) do update set
    created_at_millis = excluded.created_at_millis;

create or replace function public.prune_poi_private_activity()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    if not new.share_plans_to_friends then
        delete from public.social_activity
        where actor_id = new.id and activity_type in ('interested', 'going');
    end if;
    if not new.share_friend_activity then
        delete from public.social_activity
        where actor_id = new.id and activity_type in ('here', 'hosting');
    end if;
    return new;
end;
$$;

revoke all on function public.prune_poi_private_activity() from public;
drop trigger if exists prune_poi_private_activity on public.profiles;
create trigger prune_poi_private_activity
after update of share_plans_to_friends, share_friend_activity on public.profiles
for each row execute procedure public.prune_poi_private_activity();

alter table public.public_profile_directory enable row level security;
alter table public.friendships enable row level security;
alter table public.event_invitations enable row level security;
alter table public.social_activity enable row level security;

drop policy if exists "public_profile_directory_read" on public.public_profile_directory;
create policy "public_profile_directory_read" on public.public_profile_directory
for select to authenticated using (true);

drop policy if exists "friendships_read_participants" on public.friendships;
create policy "friendships_read_participants" on public.friendships
for select to authenticated using (auth.uid() in (requester_id, addressee_id));
drop policy if exists "friendships_insert_requester" on public.friendships;
create policy "friendships_insert_requester" on public.friendships
for insert to authenticated with check (requester_id = auth.uid());
drop policy if exists "friendships_update_recipient" on public.friendships;
create policy "friendships_update_recipient" on public.friendships
for update to authenticated using (addressee_id = auth.uid() and status = 'pending')
with check (addressee_id = auth.uid());
drop policy if exists "friendships_delete_participants" on public.friendships;
create policy "friendships_delete_participants" on public.friendships
for delete to authenticated using (auth.uid() in (requester_id, addressee_id));

drop policy if exists "event_invitations_read_participants" on public.event_invitations;
create policy "event_invitations_read_participants" on public.event_invitations
for select to authenticated using (auth.uid() in (inviter_id, invitee_id));
drop policy if exists "event_invitations_insert_inviter" on public.event_invitations;
create policy "event_invitations_insert_inviter" on public.event_invitations
for insert to authenticated with check (
    inviter_id = auth.uid() and public.are_poi_friends(inviter_id, invitee_id)
);
drop policy if exists "event_invitations_update_invitee" on public.event_invitations;
create policy "event_invitations_update_invitee" on public.event_invitations
for update to authenticated using (invitee_id = auth.uid() and status = 'pending')
with check (invitee_id = auth.uid());
drop policy if exists "event_invitations_delete_participants" on public.event_invitations;
create policy "event_invitations_delete_participants" on public.event_invitations
for delete to authenticated using (auth.uid() in (inviter_id, invitee_id));

drop policy if exists "social_activity_read_visible" on public.social_activity;
create policy "social_activity_read_visible" on public.social_activity
for select to authenticated using (
    actor_id = auth.uid()
    or (
        public.are_poi_friends(actor_id, auth.uid())
        and public.can_share_poi_activity(actor_id, activity_type)
        and exists (
            select 1 from public.events visible_event
            where visible_event.id = social_activity.event_id
              and (
                  visible_event.visibility = 'public'
                  or visible_event.created_by = auth.uid()
                  or exists (
                      select 1 from public.event_members membership
                      where membership.event_id = visible_event.id
                        and membership.user_id = auth.uid()
                  )
              )
        )
    )
);

revoke all on public.public_profile_directory, public.friendships,
    public.event_invitations, public.social_activity from anon, authenticated;
grant select on public.public_profile_directory to authenticated;
grant select, insert, update, delete on public.friendships to authenticated;
grant select, insert, update, delete on public.event_invitations to authenticated;
grant select on public.social_activity to authenticated;
grant update (share_plans_to_friends, share_friend_activity) on public.profiles to authenticated;

do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public'
          and tablename = 'public_profile_directory'
    ) then
        alter publication supabase_realtime add table public.public_profile_directory;
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'friendships'
    ) then
        alter publication supabase_realtime add table public.friendships;
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public'
          and tablename = 'event_invitations'
    ) then
        alter publication supabase_realtime add table public.event_invitations;
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'social_activity'
    ) then
        alter publication supabase_realtime add table public.social_activity;
    end if;
end $$;
