begin;

create or replace function public.can_read_poi_event(target_event_id uuid)
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select exists (
        select 1
        from public.events event
        where event.id = target_event_id
          and (
              event.visibility = 'public'
              or event.created_by = auth.uid()
              or public.is_poi_admin()
          )
    );
$$;

create or replace function public.can_contribute_poi_moment(target_event_id uuid)
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select public.is_poi_admin() or exists (
        select 1
        from public.attendance attendance
        where attendance.event_id = target_event_id
          and attendance.user_id = auth.uid()
          and attendance.status in ('here', 'attended')
    );
$$;

create or replace function public.poi_storage_event_id(object_name text)
returns uuid
language plpgsql
stable
security definer set search_path = ''
as $$
begin
    return nullif(split_part(object_name, '/', 1), '')::uuid;
exception when invalid_text_representation then
    return null;
end;
$$;

revoke all on function public.can_read_poi_event(uuid) from public;
revoke all on function public.can_contribute_poi_moment(uuid) from public;
revoke all on function public.poi_storage_event_id(text) from public;
grant execute on function public.can_read_poi_event(uuid) to anon, authenticated;
grant execute on function public.can_contribute_poi_moment(uuid) to authenticated;
grant execute on function public.poi_storage_event_id(text) to anon, authenticated;

create table public.event_moments (
    id uuid primary key default gen_random_uuid(),
    event_id uuid not null references public.events(id) on delete cascade,
    author_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    author_name text not null check (char_length(author_name) between 1 and 60),
    image_path text not null unique check (char_length(image_path) between 1 and 500),
    caption text not null default '' check (char_length(caption) <= 500),
    like_count integer not null default 0 check (like_count >= 0),
    comment_count integer not null default 0 check (comment_count >= 0),
    view_count integer not null default 0 check (view_count >= 0),
    created_at_millis bigint not null,
    created_at timestamptz not null default now()
);

create table public.moment_likes (
    moment_id uuid not null references public.event_moments(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    created_at timestamptz not null default now(),
    primary key (moment_id, user_id)
);

create table public.moment_comments (
    id uuid primary key default gen_random_uuid(),
    moment_id uuid not null references public.event_moments(id) on delete cascade,
    author_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    author_name text not null check (char_length(author_name) between 1 and 60),
    body text not null check (char_length(body) between 1 and 500),
    created_at_millis bigint not null,
    created_at timestamptz not null default now()
);

create table public.moment_views (
    moment_id uuid not null references public.event_moments(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    created_at timestamptz not null default now(),
    primary key (moment_id, user_id)
);

create index event_moments_event_created_idx
on public.event_moments (event_id, created_at_millis desc);
create index moment_comments_moment_created_idx
on public.moment_comments (moment_id, created_at_millis);

create or replace function public.set_poi_moment_author()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication is required';
    end if;
    new.author_id := auth.uid();
    select profile.display_name into new.author_name
    from public.profiles profile
    where profile.id = auth.uid();
    new.author_name := coalesce(nullif(new.author_name, ''), 'Poi member');
    return new;
end;
$$;

create or replace function public.set_poi_comment_author()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    if auth.uid() is null then
        raise exception 'Authentication is required';
    end if;
    new.author_id := auth.uid();
    select profile.display_name into new.author_name
    from public.profiles profile
    where profile.id = auth.uid();
    new.author_name := coalesce(nullif(new.author_name, ''), 'Poi member');
    return new;
end;
$$;

create or replace function public.sync_poi_moment_like_count()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare target_moment_id uuid := coalesce(new.moment_id, old.moment_id);
begin
    update public.event_moments moment
    set like_count = (
        select count(*)::integer from public.moment_likes likes
        where likes.moment_id = target_moment_id
    )
    where moment.id = target_moment_id;
    return coalesce(new, old);
end;
$$;

create or replace function public.sync_poi_moment_comment_count()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare target_moment_id uuid := coalesce(new.moment_id, old.moment_id);
begin
    update public.event_moments moment
    set comment_count = (
        select count(*)::integer from public.moment_comments comments
        where comments.moment_id = target_moment_id
    )
    where moment.id = target_moment_id;
    return coalesce(new, old);
end;
$$;

create or replace function public.sync_poi_moment_view_count()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    update public.event_moments moment
    set view_count = (
        select count(*)::integer from public.moment_views views
        where views.moment_id = new.moment_id
    )
    where moment.id = new.moment_id;
    return new;
end;
$$;

revoke all on function public.set_poi_moment_author() from public;
revoke all on function public.set_poi_comment_author() from public;
revoke all on function public.sync_poi_moment_like_count() from public;
revoke all on function public.sync_poi_moment_comment_count() from public;
revoke all on function public.sync_poi_moment_view_count() from public;

create trigger set_poi_moment_author
before insert on public.event_moments
for each row execute procedure public.set_poi_moment_author();

create trigger set_poi_comment_author
before insert on public.moment_comments
for each row execute procedure public.set_poi_comment_author();

create trigger sync_poi_moment_like_count
after insert or delete on public.moment_likes
for each row execute procedure public.sync_poi_moment_like_count();

create trigger sync_poi_moment_comment_count
after insert or delete on public.moment_comments
for each row execute procedure public.sync_poi_moment_comment_count();

create trigger sync_poi_moment_view_count
after insert on public.moment_views
for each row execute procedure public.sync_poi_moment_view_count();

alter table public.event_moments enable row level security;
alter table public.moment_likes enable row level security;
alter table public.moment_comments enable row level security;
alter table public.moment_views enable row level security;

create policy "event_moments_visible_event_read"
on public.event_moments for select to anon, authenticated
using (public.can_read_poi_event(event_id));

create policy "event_moments_attendee_create"
on public.event_moments for insert to authenticated
with check (
    author_id = auth.uid()
    and public.can_read_poi_event(event_id)
    and public.can_contribute_poi_moment(event_id)
);

create policy "event_moments_author_delete"
on public.event_moments for delete to authenticated
using (author_id = auth.uid() or public.is_poi_admin());

create policy "moment_likes_own_read"
on public.moment_likes for select to authenticated
using (user_id = auth.uid());
create policy "moment_likes_own_create"
on public.moment_likes for insert to authenticated
with check (
    user_id = auth.uid()
    and exists (
        select 1 from public.event_moments moment
        where moment.id = moment_id and public.can_read_poi_event(moment.event_id)
    )
);
create policy "moment_likes_own_delete"
on public.moment_likes for delete to authenticated
using (user_id = auth.uid());

create policy "moment_comments_visible_event_read"
on public.moment_comments for select to anon, authenticated
using (
    exists (
        select 1 from public.event_moments moment
        where moment.id = moment_id and public.can_read_poi_event(moment.event_id)
    )
);
create policy "moment_comments_member_create"
on public.moment_comments for insert to authenticated
with check (
    author_id = auth.uid()
    and exists (
        select 1 from public.event_moments moment
        where moment.id = moment_id and public.can_read_poi_event(moment.event_id)
    )
);
create policy "moment_comments_author_delete"
on public.moment_comments for delete to authenticated
using (author_id = auth.uid() or public.is_poi_admin());

create policy "moment_views_member_create"
on public.moment_views for insert to authenticated
with check (
    user_id = auth.uid()
    and exists (
        select 1 from public.event_moments moment
        where moment.id = moment_id and public.can_read_poi_event(moment.event_id)
    )
);

revoke all on public.event_moments, public.moment_likes,
    public.moment_comments, public.moment_views from anon, authenticated;
grant select on public.event_moments, public.moment_comments to anon;
grant select, insert, delete on public.event_moments to authenticated;
grant select, insert, delete on public.moment_likes to authenticated;
grant select, insert, delete on public.moment_comments to authenticated;
grant insert on public.moment_views to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'event-moments',
    'event-moments',
    false,
    6291456,
    array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create policy "event_moment_images_visible_event_read"
on storage.objects for select to anon, authenticated
using (
    bucket_id = 'event-moments'
    and public.can_read_poi_event(public.poi_storage_event_id(name))
);

create policy "event_moment_images_attendee_create"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'event-moments'
    and (storage.foldername(name))[2] = auth.uid()::text
    and public.can_contribute_poi_moment(public.poi_storage_event_id(name))
);

create policy "event_moment_images_owner_delete"
on storage.objects for delete to authenticated
using (
    bucket_id = 'event-moments'
    and (owner_id = auth.uid()::text or public.is_poi_admin())
);

do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'event_moments'
    ) then
        alter publication supabase_realtime add table public.event_moments;
    end if;
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'moment_comments'
    ) then
        alter publication supabase_realtime add table public.moment_comments;
    end if;
end $$;

commit;
