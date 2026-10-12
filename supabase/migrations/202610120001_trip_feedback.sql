begin;
create table if not exists public.tester_feedback (
 id uuid primary key default gen_random_uuid(),
 user_id uuid not null default auth.uid() references public.profiles(id) on delete cascade,
 category text not null check(category in ('login','photos','expenses','events','other')),
 rating integer not null check(rating between 1 and 5),
 message text not null check(length(trim(message)) between 10 and 2000),
 platform text not null check(platform in ('android','web')),
 app_version text not null check(length(app_version) between 1 and 40),
 created_at timestamptz not null default now()
);
alter table public.tester_feedback enable row level security;
revoke all on public.tester_feedback from anon, authenticated;
grant select, insert on public.tester_feedback to authenticated;
create policy feedback_insert_self on public.tester_feedback for insert to authenticated
 with check(user_id=auth.uid());
create policy feedback_read_self_or_admin on public.tester_feedback for select to authenticated
 using(user_id=auth.uid() or public.is_poi_admin());
create index if not exists tester_feedback_user_date on public.tester_feedback(user_id,created_at desc);
do $$ begin
 if not exists(select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename='tester_feedback') then
  alter publication supabase_realtime add table public.tester_feedback;
 end if;
end $$;
commit;
