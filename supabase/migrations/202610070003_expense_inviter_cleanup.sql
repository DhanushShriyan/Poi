begin;
create or replace function public.prepare_poi_expense_member()
returns trigger language plpgsql security definer set search_path = ''
as $$
begin
    if tg_op = 'INSERT' then
        if not public.can_manage_poi_expense_group(new.group_id) then
            raise exception 'Only the expense-group organizer can invite members.';
        end if;
        select display_name into new.display_name from public.profiles where id = new.user_id;
        if new.user_id = auth.uid() then
            new.status := 'active'; new.invited_by := auth.uid(); return new;
        end if;
        if not public.are_poi_friends(auth.uid(), new.user_id) then
            raise exception 'Invite an accepted friend to this expense group.';
        end if;
        new.status := 'invited'; new.invited_by := auth.uid(); return new;
    end if;
    -- The FK clears the inviter when their profile is deleted. Clients have no
    -- UPDATE privilege on invited_by, and cannot use this path to edit members.
    if old.invited_by is not null and new.invited_by is null
       and not exists (select 1 from public.profiles where id = old.invited_by)
       and old.group_id = new.group_id and old.user_id = new.user_id
       and old.display_name = new.display_name and old.status = new.status
       and old.upi_id is not distinct from new.upi_id then return new; end if;
    if old.group_id <> new.group_id or old.user_id <> new.user_id
       or old.display_name <> new.display_name or old.invited_by is distinct from new.invited_by then
        raise exception 'Expense membership identity cannot be changed.';
    end if;
    if auth.uid() <> old.user_id then
        raise exception 'Only this member can update their expense membership.';
    end if;
    if old.status = 'invited' and new.status not in ('active', 'declined') then
        raise exception 'Accept or decline the expense invitation.';
    end if;
    if old.status <> 'invited' and new.status <> old.status then
        raise exception 'Expense membership status cannot be changed again.';
    end if;
    new.updated_at_millis := floor(extract(epoch from now()) * 1000)::bigint;
    return new;
end;
$$;
commit;
