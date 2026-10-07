begin;

-- An INSERT RETURNING policy must recognize the creator directly, before the
-- statement's snapshot can query the newly inserted group.
drop policy expense_groups_member_read on public.event_expense_groups;
create policy expense_groups_member_read on public.event_expense_groups
for select to authenticated using (
    created_by = auth.uid() or public.can_view_poi_expense_group(id)
);

-- Each friend circle can have its own group at the same public event.
alter table public.event_expense_groups drop constraint event_expense_groups_event_id_key;
alter table public.event_expense_groups add constraint event_expense_groups_event_creator_key
unique (event_id, created_by);

-- Preserve community listings when their author deletes their account. A normal
-- client cannot clear or reassign ownership while the author's profile exists.
create or replace function public.protect_poi_event_fields()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    if old.created_by is distinct from new.created_by and not (
        new.created_by is null and old.created_by is not null
        and not exists (select 1 from public.profiles where id = old.created_by)
    ) then
        raise exception 'created_by cannot be changed';
    end if;
    if not public.is_poi_admin() then
        new.organizer_verified := old.organizer_verified;
        new.verification := old.verification;
        new.featured := old.featured;
        if pg_trigger_depth() <= 1 then new.attendee_count := old.attendee_count; end if;
    end if;
    return new;
end;
$$;
revoke all on function public.protect_poi_event_fields() from public;
commit;
