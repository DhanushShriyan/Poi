begin;

-- No financial records are deleted, merged, or recalculated by this migration.
-- Explicit accepted invitations / guest links create event_members; public
-- browsing and Interested / Going alone are deliberately not financial access.
alter table public.event_expense_members add column membership_source text not null default 'invitation'
 check (membership_source in ('invitation','event'));

create or replace function public.is_confirmed_poi_event_participant(p_event_id uuid, p_user_id uuid)
returns boolean language sql stable security definer set search_path='' as $$
 select p_user_id is not null and exists (
  select 1 from public.events e where e.id=p_event_id and
   (e.created_by=p_user_id or exists(select 1 from public.event_members m where m.event_id=e.id and m.user_id=p_user_id))
 );
$$;
revoke all on function public.is_confirmed_poi_event_participant(uuid,uuid) from public,anon,authenticated;

create or replace function public.prepare_poi_expense_member()
returns trigger language plpgsql security definer set search_path='' as $$
declare v_event uuid; v_creator uuid; v_eligible boolean;
begin
 select event_id,created_by into v_event,v_creator from public.event_expense_groups where id=new.group_id;
 v_eligible := public.is_confirmed_poi_event_participant(v_event,new.user_id);
 if tg_op='INSERT' then
  select display_name into new.display_name from public.profiles where id=new.user_id;
  if new.membership_source='event' and v_eligible then
   new.status:='active'; new.invited_by:=v_creator; return new;
  end if;
  if not public.can_manage_poi_expense_group(new.group_id) then raise exception 'Only the expense organizer can invite members.'; end if;
  if new.user_id=auth.uid() then new.status:='active';new.invited_by:=auth.uid();return new;end if;
  if not public.are_poi_friends(auth.uid(),new.user_id) then raise exception 'Invite an accepted friend or have the guest join this event.';end if;
  new.status:='invited';new.invited_by:=auth.uid();return new;
 end if;
 -- Keep the existing account-FK path compatible; this is not cleanup work.
 if old.invited_by is not null and new.invited_by is null
  and not exists(select 1 from public.profiles where id=old.invited_by)
  and old.group_id=new.group_id and old.user_id=new.user_id and old.display_name=new.display_name
  and old.status=new.status and old.upi_id is not distinct from new.upi_id
  and old.membership_source=new.membership_source then return new;end if;
 if v_eligible and old.status<>'active' and new.status='active' and new.membership_source='event'
  and old.group_id=new.group_id and old.user_id=new.user_id and old.display_name=new.display_name
  and old.invited_by is not distinct from new.invited_by and old.upi_id is not distinct from new.upi_id
  and (auth.uid() is null or pg_trigger_depth()>1) then
  new.updated_at_millis:=floor(extract(epoch from now())*1000)::bigint;return new;
 end if;
 if old.group_id<>new.group_id or old.user_id<>new.user_id or old.display_name<>new.display_name
  or old.invited_by is distinct from new.invited_by or old.membership_source<>new.membership_source then
  raise exception 'Expense membership identity cannot be changed.';
 end if;
 if v_eligible and new.status='active' and new.upi_id is not distinct from old.upi_id
  and (auth.uid() is null or pg_trigger_depth()>1) then return new;end if;
 if auth.uid()<>old.user_id then raise exception 'Only this member can update their membership.';end if;
 if old.status='invited' and new.status not in ('active','declined') then raise exception 'Accept or decline the invitation.';end if;
 if old.status<>'invited' and new.status<>old.status then raise exception 'Membership status cannot be changed again.';end if;
 new.updated_at_millis:=floor(extract(epoch from now())*1000)::bigint;
 return new;
end;
$$;

create or replace function public.sync_poi_event_expense_participants(p_event_id uuid)
returns void language sql security definer set search_path='' as $$
 insert into public.event_expense_members(group_id,user_id,display_name,status,invited_by,membership_source)
 select g.id,p.id,p.display_name,'active',g.created_by,'event'
 from public.event_expense_groups g join public.events e on e.id=g.event_id
 join public.profiles p on p.id=e.created_by or exists(select 1 from public.event_members m where m.event_id=e.id and m.user_id=p.id)
 where e.id=p_event_id
 on conflict(group_id,user_id) do update set status='active', membership_source='event'
 where public.event_expense_members.status<>'active';
$$;
revoke all on function public.sync_poi_event_expense_participants(uuid) from public,anon,authenticated;

create or replace function public.on_poi_event_expense_participant()
returns trigger language plpgsql security definer set search_path='' as $$
begin perform public.sync_poi_event_expense_participants(new.event_id);return new;end;
$$;
revoke all on function public.on_poi_event_expense_participant() from public,anon,authenticated;
create trigger sync_event_members_to_expenses after insert on public.event_members
 for each row execute function public.on_poi_event_expense_participant();
create trigger sync_new_expense_group_event_members after insert on public.event_expense_groups
 for each row execute function public.on_poi_event_expense_participant();

create or replace function public.is_active_poi_expense_member(target_group_id uuid)
returns boolean language sql stable security definer set search_path='' as $$
 select public.is_poi_admin() or exists (
  select 1 from public.event_expense_groups g join public.event_expense_members m on m.group_id=g.id
  where g.id=target_group_id and m.user_id=auth.uid() and m.status='active'
   and (m.membership_source='invitation' or public.is_confirmed_poi_event_participant(g.event_id,auth.uid()))
 );
$$;
create or replace function public.can_view_poi_expense_group(target_group_id uuid)
returns boolean language sql stable security definer set search_path='' as $$
 select public.is_poi_admin() or exists (
  select 1 from public.event_expense_groups g left join public.event_expense_members m on m.group_id=g.id and m.user_id=auth.uid()
  where g.id=target_group_id and (g.created_by=auth.uid() or public.is_confirmed_poi_event_participant(g.event_id,auth.uid())
   or (m.membership_source='invitation' and m.status in ('invited','active')))
 );
$$;

-- Populate missing membership rows for EXISTING events, including Goa. All
-- expense amounts, payer/share rows, receipts and settlements stay untouched.
do $$ declare v_event uuid; begin
 for v_event in select distinct event_id from public.event_expense_groups loop
  perform public.sync_poi_event_expense_participants(v_event);
 end loop;
end $$;

create or replace function public.get_or_start_poi_event_expense_group(p_event_id uuid,p_currency text default 'INR')
returns uuid language plpgsql security definer set search_path='' as $$
declare v_group uuid;
begin
 if auth.uid() is null or not public.is_confirmed_poi_event_participant(p_event_id,auth.uid()) then
  raise exception 'Join this event through its invitation or guest join link before starting shared expenses.';
 end if;
 perform pg_advisory_xact_lock(hashtextextended(p_event_id::text,0));
 select id into v_group from public.event_expense_groups where event_id=p_event_id order by created_at_millis,id limit 1;
 if v_group is null then
  insert into public.event_expense_groups(event_id,created_by,currency) values(p_event_id,auth.uid(),p_currency) returning id into v_group;
 end if;
 return v_group;
end;
$$;
revoke all on function public.get_or_start_poi_event_expense_group(uuid,text) from public,anon;
grant execute on function public.get_or_start_poi_event_expense_group(uuid,text) to authenticated;
commit;
