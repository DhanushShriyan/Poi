begin;

-- Photo posting is independent of attendance. Private-event access remains required.
create or replace function public.can_contribute_poi_moment(target_event_id uuid)
returns boolean language sql stable security definer set search_path = '' as $$
 select auth.uid() is not null and public.can_read_poi_event(target_event_id);
$$;

-- Only hashes are stored. A join link is an explicit owner-issued invitation,
-- not a way to expose every private event or its expenses to the public.
create table public.event_join_links (
 event_id uuid primary key references public.events(id) on delete cascade,
 token_hash text not null unique,
 expires_at timestamptz not null,
 created_by uuid not null references public.profiles(id) on delete cascade
);
alter table public.event_join_links enable row level security;
revoke all on public.event_join_links from public, anon, authenticated;

create or replace function public.create_poi_event_join_link(p_event_id uuid)
returns text language plpgsql security definer set search_path = '' as $$
declare v_token text;
begin
 if auth.uid() is null or not exists(select 1 from public.events where id=p_event_id and created_by=auth.uid() and not is_cancelled) then
  raise exception 'Only the owner of an active event can create a join link.';
 end if;
 v_token := replace(gen_random_uuid()::text,'-','') || replace(gen_random_uuid()::text,'-','');
 insert into public.event_join_links(event_id,token_hash,expires_at,created_by)
 values(p_event_id,encode(sha256(convert_to(v_token,'UTF8')),'hex'),now()+interval '30 days',auth.uid())
 on conflict(event_id) do update set token_hash=excluded.token_hash,expires_at=excluded.expires_at,created_by=excluded.created_by;
 return v_token;
end;
$$;

create or replace function public.revoke_poi_event_join_link(p_event_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
 if not exists(select 1 from public.events where id=p_event_id and created_by=auth.uid()) then raise exception 'Only the owner can revoke a join link.'; end if;
 delete from public.event_join_links where event_id=p_event_id;
end;
$$;

create or replace function public.join_poi_event_link(p_token text)
returns uuid language plpgsql security definer set search_path = '' as $$
declare v_link public.event_join_links%rowtype;
begin
 if auth.uid() is null then raise exception 'Sign in to join this event.'; end if;
 if p_token is null or p_token !~ '^[a-f0-9]{64}$' then raise exception 'Invalid or expired join link.'; end if;
 select * into v_link from public.event_join_links where token_hash=encode(sha256(convert_to(p_token,'UTF8')),'hex') and expires_at>now() for share;
 if v_link.event_id is null or not exists(select 1 from public.events where id=v_link.event_id and not is_cancelled) then raise exception 'Invalid or expired join link.'; end if;
 if auth.uid()<>v_link.created_by then
  insert into public.event_members(event_id,user_id,invited_by) values(v_link.event_id,auth.uid(),v_link.created_by) on conflict do nothing;
 end if;
 return v_link.event_id;
end;
$$;
revoke all on function public.create_poi_event_join_link(uuid), public.revoke_poi_event_join_link(uuid), public.join_poi_event_link(text) from public, anon;
grant execute on function public.create_poi_event_join_link(uuid), public.revoke_poi_event_join_link(uuid), public.join_poi_event_link(text) to authenticated;
commit;
