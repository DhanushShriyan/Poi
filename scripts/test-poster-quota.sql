-- Transactional checks: no changes to accounts, events or quota records survive.
begin;
do $$
declare test_user uuid;
begin
  select id into test_user from auth.users u
  where not exists(select 1 from public.poster_scan_usage s where s.user_id = u.id)
  limit 1;
  if test_user is null then raise exception 'No unused identity for transactional quota test'; end if;
  insert into public.poster_scan_usage(user_id, used_at)
    select test_user, now() - interval '2 minutes' from generate_series(1, 5);
  if public.reserve_poster_scan(test_user) then raise exception 'Per-user daily cap failed'; end if;
  insert into public.poster_scan_usage(user_id, used_at)
    select null, now() from generate_series(1, 8);
  if public.reserve_poster_scan(null) then raise exception 'Global minute cap failed'; end if;
  insert into public.poster_scan_usage(user_id, used_at)
    select null, now() - interval '2 minutes' from generate_series(1, 100);
  if public.reserve_poster_scan(null) then raise exception 'Global daily cap failed'; end if;
end;
$$;
rollback;
select 'PASS: daily user, global minute and global daily caps; all test writes rolled back' as result;
