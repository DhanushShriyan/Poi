-- Only the authenticated Edge Function may reserve quota. No poster data is stored.
create table if not exists public.poster_scan_usage (
  user_id uuid references auth.users(id) on delete set null,
  used_at timestamptz not null default now()
);
-- Account deletion removes the identifier but must not reset the shared daily budget.
alter table public.poster_scan_usage alter column user_id drop not null;
alter table public.poster_scan_usage drop constraint if exists poster_scan_usage_user_id_fkey;
alter table public.poster_scan_usage add constraint poster_scan_usage_user_id_fkey
  foreign key(user_id) references auth.users(id) on delete set null;
alter table public.poster_scan_usage enable row level security;
revoke all on public.poster_scan_usage from anon, authenticated;
create index if not exists poster_scan_usage_time on public.poster_scan_usage(used_at);

create or replace function public.reserve_poster_scan(scan_user uuid)
returns boolean language plpgsql security definer set search_path = public as $$
begin
  -- Serializes global and per-user checks across all function instances.
  perform pg_advisory_xact_lock(764820061);
  if (select count(*) from public.poster_scan_usage where used_at >= date_trunc('day', now() at time zone 'UTC') at time zone 'UTC') >= 100
     or (select count(*) from public.poster_scan_usage where used_at > now() - interval '1 minute') >= 8
     or (select count(*) from public.poster_scan_usage where user_id = scan_user and used_at >= date_trunc('day', now() at time zone 'UTC') at time zone 'UTC') >= 5
     or exists(select 1 from public.poster_scan_usage where user_id = scan_user and used_at > now() - interval '15 seconds') then
    return false;
  end if;
  insert into public.poster_scan_usage(user_id) values(scan_user);
  return true;
end;
$$;
revoke all on function public.reserve_poster_scan(uuid) from public, anon, authenticated;
grant execute on function public.reserve_poster_scan(uuid) to service_role;
