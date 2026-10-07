begin;

-- Event-scoped shared expenses. Poi never holds money: settlements are records
-- confirmed by both sides, while payment apps are opened externally.
create table public.event_expense_groups (
    id uuid primary key default gen_random_uuid(),
    event_id uuid not null unique references public.events(id) on delete cascade,
    created_by uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    currency text not null default 'INR' check (currency ~ '^[A-Z]{3}$'),
    created_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    updated_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint
);

create table public.event_expense_members (
    group_id uuid not null references public.event_expense_groups(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    display_name text not null,
    status text not null default 'invited' check (status in ('invited', 'active', 'declined')),
    upi_id text check (upi_id is null or length(upi_id) between 5 and 100),
    invited_by uuid references public.profiles(id) on delete set null default auth.uid(),
    created_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    updated_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    primary key (group_id, user_id)
);

create table public.event_expenses (
    id uuid primary key,
    group_id uuid not null references public.event_expense_groups(id) on delete cascade,
    created_by uuid references public.profiles(id) on delete set null default auth.uid(),
    created_by_name text not null,
    title text not null check (length(title) between 1 and 100),
    note text not null default '' check (length(note) <= 500),
    category text not null check (category in ('tickets', 'travel', 'food', 'stay', 'shopping', 'other')),
    currency text not null check (currency ~ '^[A-Z]{3}$'),
    amount_minor bigint not null check (amount_minor > 0 and amount_minor <= 100000000000),
    split_method text not null check (split_method in ('equal', 'exact', 'percentage', 'shares')),
    receipt_path text,
    created_at_millis bigint not null,
    updated_at_millis bigint not null,
    deleted_at_millis bigint
);

create table public.event_expense_payments (
    expense_id uuid not null references public.event_expenses(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    amount_minor bigint not null check (amount_minor > 0),
    primary key (expense_id, user_id)
);

create table public.event_expense_shares (
    expense_id uuid not null references public.event_expenses(id) on delete cascade,
    user_id uuid not null references public.profiles(id) on delete cascade,
    amount_minor bigint not null check (amount_minor >= 0),
    primary key (expense_id, user_id)
);

create table public.event_expense_comments (
    id uuid primary key default gen_random_uuid(),
    expense_id uuid not null references public.event_expenses(id) on delete cascade,
    author_id uuid not null references public.profiles(id) on delete cascade default auth.uid(),
    author_name text not null,
    body text not null check (length(body) between 1 and 500),
    created_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint
);

create table public.event_expense_settlements (
    id uuid primary key default gen_random_uuid(),
    group_id uuid not null references public.event_expense_groups(id) on delete cascade,
    payer_id uuid not null references public.profiles(id) on delete cascade,
    payee_id uuid not null references public.profiles(id) on delete cascade,
    payer_name text not null,
    payee_name text not null,
    amount_minor bigint not null check (amount_minor > 0),
    currency text not null check (currency ~ '^[A-Z]{3}$'),
    note text not null default '' check (length(note) <= 300),
    status text not null default 'pending' check (status in ('pending', 'confirmed')),
    created_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint,
    confirmed_at_millis bigint,
    check (payer_id <> payee_id)
);

create table public.event_expense_activity (
    id uuid primary key default gen_random_uuid(),
    group_id uuid not null references public.event_expense_groups(id) on delete cascade,
    actor_id uuid references public.profiles(id) on delete set null,
    actor_name text not null,
    action text not null check (action in ('expense_added', 'expense_updated', 'expense_deleted', 'settlement_recorded', 'settlement_confirmed')),
    subject_id uuid not null,
    summary text not null check (length(summary) <= 220),
    created_at_millis bigint not null default floor(extract(epoch from now()) * 1000)::bigint
);

create index event_expense_members_user_idx on public.event_expense_members(user_id, status);
create index event_expenses_group_idx on public.event_expenses(group_id, created_at_millis desc);
create index event_expense_payments_user_idx on public.event_expense_payments(user_id);
create index event_expense_shares_user_idx on public.event_expense_shares(user_id);
create index event_expense_comments_expense_idx on public.event_expense_comments(expense_id, created_at_millis);
create index event_expense_settlements_group_idx on public.event_expense_settlements(group_id, created_at_millis desc);
create index event_expense_activity_group_idx on public.event_expense_activity(group_id, created_at_millis desc);

create or replace function public.can_view_poi_expense_group(target_group_id uuid)
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select public.is_poi_admin() or exists (
        select 1
        from public.event_expense_groups expense_group
        left join public.event_expense_members member
          on member.group_id = expense_group.id and member.user_id = auth.uid()
        where expense_group.id = target_group_id
          and (expense_group.created_by = auth.uid() or member.status in ('invited', 'active'))
    );
$$;

create or replace function public.is_active_poi_expense_member(target_group_id uuid)
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select public.is_poi_admin() or exists (
        select 1 from public.event_expense_members member
        where member.group_id = target_group_id
          and member.user_id = auth.uid()
          and member.status = 'active'
    );
$$;

create or replace function public.can_manage_poi_expense_group(target_group_id uuid)
returns boolean
language sql
stable
security definer set search_path = ''
as $$
    select public.is_poi_admin() or exists (
        select 1 from public.event_expense_groups expense_group
        where expense_group.id = target_group_id and expense_group.created_by = auth.uid()
    );
$$;

revoke all on function public.can_view_poi_expense_group(uuid) from public;
revoke all on function public.is_active_poi_expense_member(uuid) from public;
revoke all on function public.can_manage_poi_expense_group(uuid) from public;
grant execute on function public.can_view_poi_expense_group(uuid) to authenticated;
grant execute on function public.is_active_poi_expense_member(uuid) to authenticated;
grant execute on function public.can_manage_poi_expense_group(uuid) to authenticated;

create or replace function public.start_poi_expense_group()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare creator_name text;
begin
    if auth.uid() is null or new.created_by <> auth.uid() then
        raise exception 'Sign in to start event expenses.';
    end if;
    if not public.can_read_poi_event(new.event_id) then
        raise exception 'You cannot access this event.';
    end if;
    select profile.display_name into creator_name
    from public.profiles profile where profile.id = auth.uid();
    insert into public.event_expense_members (
        group_id, user_id, display_name, status, invited_by,
        created_at_millis, updated_at_millis
    ) values (
        new.id, auth.uid(), coalesce(creator_name, 'Poi member'), 'active', auth.uid(),
        new.created_at_millis, new.created_at_millis
    );
    return new;
end;
$$;

drop trigger if exists start_poi_expense_group on public.event_expense_groups;
create trigger start_poi_expense_group
after insert on public.event_expense_groups
for each row execute procedure public.start_poi_expense_group();

create or replace function public.prepare_poi_expense_member()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
begin
    if tg_op = 'INSERT' then
        if not public.can_manage_poi_expense_group(new.group_id) then
            raise exception 'Only the expense-group organizer can invite members.';
        end if;
        select profile.display_name into new.display_name
        from public.profiles profile where profile.id = new.user_id;
        if new.user_id = auth.uid() then
            new.status := 'active';
            new.invited_by := auth.uid();
            return new;
        end if;
        if not public.are_poi_friends(auth.uid(), new.user_id) then
            raise exception 'Invite an accepted friend to this expense group.';
        end if;
        new.status := 'invited';
        new.invited_by := auth.uid();
        return new;
    end if;

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

drop trigger if exists prepare_poi_expense_member on public.event_expense_members;
create trigger prepare_poi_expense_member
before insert or update on public.event_expense_members
for each row execute procedure public.prepare_poi_expense_member();

create or replace function public.prepare_poi_expense_comment()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare target_group uuid;
begin
    select expense.group_id into target_group
    from public.event_expenses expense
    where expense.id = new.expense_id and expense.deleted_at_millis is null;
    if target_group is null or not public.is_active_poi_expense_member(target_group) then
        raise exception 'Join this expense group before commenting.';
    end if;
    new.author_id := auth.uid();
    select profile.display_name into new.author_name
    from public.profiles profile where profile.id = auth.uid();
    return new;
end;
$$;

drop trigger if exists prepare_poi_expense_comment on public.event_expense_comments;
create trigger prepare_poi_expense_comment
before insert on public.event_expense_comments
for each row execute procedure public.prepare_poi_expense_comment();

create or replace function public.create_poi_event_expense(
    p_expense_id uuid,
    p_group_id uuid,
    p_title text,
    p_note text,
    p_category text,
    p_currency text,
    p_amount_minor bigint,
    p_split_method text,
    p_payments jsonb,
    p_shares jsonb,
    p_receipt_path text,
    p_created_at_millis bigint
)
returns void
language plpgsql
security definer set search_path = ''
as $$
declare
    item jsonb;
    member_id uuid;
    item_amount bigint;
    payment_total bigint := 0;
    share_total bigint := 0;
    creator_name text;
    group_currency text;
begin
    if not public.is_active_poi_expense_member(p_group_id) then
        raise exception 'Join this expense group before adding an expense.';
    end if;
    select expense_group.currency into group_currency
    from public.event_expense_groups expense_group where expense_group.id = p_group_id;
    if group_currency is null or group_currency <> p_currency then
        raise exception 'Use the expense group currency.';
    end if;
    if p_amount_minor <= 0 or p_amount_minor > 100000000000 then
        raise exception 'Enter a valid expense amount.';
    end if;
    select profile.display_name into creator_name
    from public.profiles profile where profile.id = auth.uid();

    insert into public.event_expenses (
        id, group_id, created_by, created_by_name, title, note, category,
        currency, amount_minor, split_method, receipt_path,
        created_at_millis, updated_at_millis
    ) values (
        p_expense_id, p_group_id, auth.uid(), coalesce(creator_name, 'Poi member'),
        trim(p_title), trim(coalesce(p_note, '')), p_category, p_currency,
        p_amount_minor, p_split_method, p_receipt_path,
        p_created_at_millis, p_created_at_millis
    );

    for item in select * from jsonb_array_elements(p_payments)
    loop
        member_id := (item ->> 'user_id')::uuid;
        item_amount := (item ->> 'amount_minor')::bigint;
        if item_amount <= 0 or not exists (
            select 1 from public.event_expense_members member
            where member.group_id = p_group_id and member.user_id = member_id and member.status = 'active'
        ) then
            raise exception 'Every payer must be an active expense-group member.';
        end if;
        insert into public.event_expense_payments(expense_id, user_id, amount_minor)
        values (p_expense_id, member_id, item_amount);
        payment_total := payment_total + item_amount;
    end loop;

    for item in select * from jsonb_array_elements(p_shares)
    loop
        member_id := (item ->> 'user_id')::uuid;
        item_amount := (item ->> 'amount_minor')::bigint;
        if item_amount < 0 or not exists (
            select 1 from public.event_expense_members member
            where member.group_id = p_group_id and member.user_id = member_id and member.status = 'active'
        ) then
            raise exception 'Every participant must be an active expense-group member.';
        end if;
        insert into public.event_expense_shares(expense_id, user_id, amount_minor)
        values (p_expense_id, member_id, item_amount);
        share_total := share_total + item_amount;
    end loop;

    if payment_total <> p_amount_minor or share_total <> p_amount_minor then
        raise exception 'Payments and shares must each equal the expense total.';
    end if;
end;
$$;

create or replace function public.update_poi_event_expense(
    p_expense_id uuid,
    p_title text,
    p_note text,
    p_category text,
    p_amount_minor bigint,
    p_split_method text,
    p_payments jsonb,
    p_shares jsonb,
    p_updated_at_millis bigint
)
returns void
language plpgsql
security definer set search_path = ''
as $$
declare
    target_group uuid;
    owner_id uuid;
    item jsonb;
    member_id uuid;
    item_amount bigint;
    payment_total bigint := 0;
    share_total bigint := 0;
begin
    select expense.group_id, expense.created_by into target_group, owner_id
    from public.event_expenses expense
    where expense.id = p_expense_id and expense.deleted_at_millis is null;
    if target_group is null or not public.is_active_poi_expense_member(target_group)
       or (owner_id is distinct from auth.uid() and not public.can_manage_poi_expense_group(target_group)) then
        raise exception 'Only the expense author or group organizer can edit it.';
    end if;
    update public.event_expenses set
        title = trim(p_title), note = trim(coalesce(p_note, '')), category = p_category,
        amount_minor = p_amount_minor, split_method = p_split_method,
        updated_at_millis = p_updated_at_millis
    where id = p_expense_id;
    delete from public.event_expense_payments where expense_id = p_expense_id;
    delete from public.event_expense_shares where expense_id = p_expense_id;

    for item in select * from jsonb_array_elements(p_payments)
    loop
        member_id := (item ->> 'user_id')::uuid;
        item_amount := (item ->> 'amount_minor')::bigint;
        if item_amount <= 0 or not exists (
            select 1 from public.event_expense_members member
            where member.group_id = target_group and member.user_id = member_id and member.status = 'active'
        ) then raise exception 'Every payer must be active.'; end if;
        insert into public.event_expense_payments values (p_expense_id, member_id, item_amount);
        payment_total := payment_total + item_amount;
    end loop;
    for item in select * from jsonb_array_elements(p_shares)
    loop
        member_id := (item ->> 'user_id')::uuid;
        item_amount := (item ->> 'amount_minor')::bigint;
        if item_amount < 0 or not exists (
            select 1 from public.event_expense_members member
            where member.group_id = target_group and member.user_id = member_id and member.status = 'active'
        ) then raise exception 'Every participant must be active.'; end if;
        insert into public.event_expense_shares values (p_expense_id, member_id, item_amount);
        share_total := share_total + item_amount;
    end loop;
    if payment_total <> p_amount_minor or share_total <> p_amount_minor then
        raise exception 'Payments and shares must each equal the expense total.';
    end if;
end;
$$;

create or replace function public.delete_poi_event_expense(p_expense_id uuid)
returns void
language plpgsql
security definer set search_path = ''
as $$
declare target_group uuid; owner_id uuid;
begin
    select expense.group_id, expense.created_by into target_group, owner_id
    from public.event_expenses expense
    where expense.id = p_expense_id and expense.deleted_at_millis is null;
    if target_group is null or (owner_id is distinct from auth.uid()
       and not public.can_manage_poi_expense_group(target_group)) then
        raise exception 'Only the expense author or group organizer can delete it.';
    end if;
    update public.event_expenses
    set deleted_at_millis = floor(extract(epoch from now()) * 1000)::bigint,
        updated_at_millis = floor(extract(epoch from now()) * 1000)::bigint
    where id = p_expense_id;
end;
$$;

revoke all on function public.create_poi_event_expense(uuid, uuid, text, text, text, text, bigint, text, jsonb, jsonb, text, bigint) from public;
revoke all on function public.update_poi_event_expense(uuid, text, text, text, bigint, text, jsonb, jsonb, bigint) from public;
revoke all on function public.delete_poi_event_expense(uuid) from public;
grant execute on function public.create_poi_event_expense(uuid, uuid, text, text, text, text, bigint, text, jsonb, jsonb, text, bigint) to authenticated;
grant execute on function public.update_poi_event_expense(uuid, text, text, text, bigint, text, jsonb, jsonb, bigint) to authenticated;
grant execute on function public.delete_poi_event_expense(uuid) to authenticated;

create or replace function public.audit_poi_expense_change()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare actor_name text; action_name text;
begin
    select profile.display_name into actor_name from public.profiles profile where profile.id = auth.uid();
    if tg_op = 'INSERT' then action_name := 'expense_added';
    elsif old.deleted_at_millis is null and new.deleted_at_millis is not null then action_name := 'expense_deleted';
    else action_name := 'expense_updated'; end if;
    insert into public.event_expense_activity(group_id, actor_id, actor_name, action, subject_id, summary)
    values (
        new.group_id, auth.uid(), coalesce(actor_name, new.created_by_name), action_name, new.id,
        left(new.title || ' · ' || new.currency || ' ' || to_char(new.amount_minor / 100.0, 'FM9999999990.00'), 220)
    );
    return new;
end;
$$;

drop trigger if exists audit_poi_expense_change on public.event_expenses;
create trigger audit_poi_expense_change
after insert or update of title, note, category, amount_minor, split_method, deleted_at_millis on public.event_expenses
for each row execute procedure public.audit_poi_expense_change();

create or replace function public.prepare_poi_expense_settlement()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare group_currency text;
begin
    if tg_op = 'INSERT' then
        if new.payer_id <> auth.uid() or not public.is_active_poi_expense_member(new.group_id) then
            raise exception 'Record only your own outgoing settlement.';
        end if;
        if not exists (
            select 1 from public.event_expense_members member
            where member.group_id = new.group_id and member.user_id = new.payee_id and member.status = 'active'
        ) then raise exception 'The recipient must be an active member.'; end if;
        select expense_group.currency into group_currency
        from public.event_expense_groups expense_group where expense_group.id = new.group_id;
        if new.currency <> group_currency or new.amount_minor > 100000000000 then
            raise exception 'Use the expense group currency and a valid amount.';
        end if;
        select profile.display_name into new.payer_name from public.profiles profile where profile.id = new.payer_id;
        select profile.display_name into new.payee_name from public.profiles profile where profile.id = new.payee_id;
        new.status := 'pending';
        new.confirmed_at_millis := null;
        return new;
    end if;
    if old.payer_id <> new.payer_id or old.payee_id <> new.payee_id
       or old.amount_minor <> new.amount_minor or old.group_id <> new.group_id then
        raise exception 'Settlement details cannot be changed.';
    end if;
    if auth.uid() <> old.payee_id or old.status <> 'pending' or new.status <> 'confirmed' then
        raise exception 'Only the recipient can confirm a pending settlement.';
    end if;
    new.confirmed_at_millis := floor(extract(epoch from now()) * 1000)::bigint;
    return new;
end;
$$;

drop trigger if exists prepare_poi_expense_settlement on public.event_expense_settlements;
create trigger prepare_poi_expense_settlement
before insert or update on public.event_expense_settlements
for each row execute procedure public.prepare_poi_expense_settlement();

create or replace function public.audit_poi_expense_settlement()
returns trigger
language plpgsql
security definer set search_path = ''
as $$
declare action_name text; actor_name text;
begin
    action_name := case when tg_op = 'INSERT' then 'settlement_recorded' else 'settlement_confirmed' end;
    select profile.display_name into actor_name from public.profiles profile where profile.id = auth.uid();
    insert into public.event_expense_activity(group_id, actor_id, actor_name, action, subject_id, summary)
    values (
        new.group_id, auth.uid(), coalesce(actor_name, 'Poi member'), action_name, new.id,
        left(new.payer_name || ' → ' || new.payee_name || ' · ' || new.currency || ' ' ||
          to_char(new.amount_minor / 100.0, 'FM9999999990.00'), 220)
    );
    return new;
end;
$$;

drop trigger if exists audit_poi_expense_settlement on public.event_expense_settlements;
create trigger audit_poi_expense_settlement
after insert or update of status on public.event_expense_settlements
for each row execute procedure public.audit_poi_expense_settlement();

alter table public.event_expense_groups enable row level security;
alter table public.event_expense_members enable row level security;
alter table public.event_expenses enable row level security;
alter table public.event_expense_payments enable row level security;
alter table public.event_expense_shares enable row level security;
alter table public.event_expense_comments enable row level security;
alter table public.event_expense_settlements enable row level security;
alter table public.event_expense_activity enable row level security;

create policy "expense_groups_member_read" on public.event_expense_groups
for select to authenticated using (public.can_view_poi_expense_group(id));
create policy "expense_groups_event_member_create" on public.event_expense_groups
for insert to authenticated with check (created_by = auth.uid() and public.can_read_poi_event(event_id));
create policy "expense_groups_owner_update" on public.event_expense_groups
for update to authenticated using (public.can_manage_poi_expense_group(id))
with check (public.can_manage_poi_expense_group(id));

create policy "expense_members_group_read" on public.event_expense_members
for select to authenticated using (
    user_id = auth.uid()
    or public.is_active_poi_expense_member(group_id)
    or public.can_manage_poi_expense_group(group_id)
);
create policy "expense_members_owner_invite" on public.event_expense_members
for insert to authenticated with check (public.can_manage_poi_expense_group(group_id));
create policy "expense_members_self_update" on public.event_expense_members
for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

create policy "event_expenses_member_read" on public.event_expenses
for select to authenticated using (public.is_active_poi_expense_member(group_id));
create policy "expense_payments_member_read" on public.event_expense_payments
for select to authenticated using (exists (
    select 1 from public.event_expenses expense
    where expense.id = event_expense_payments.expense_id
      and public.is_active_poi_expense_member(expense.group_id)
));
create policy "expense_shares_member_read" on public.event_expense_shares
for select to authenticated using (exists (
    select 1 from public.event_expenses expense
    where expense.id = event_expense_shares.expense_id
      and public.is_active_poi_expense_member(expense.group_id)
));
create policy "expense_comments_member_read" on public.event_expense_comments
for select to authenticated using (exists (
    select 1 from public.event_expenses expense
    where expense.id = event_expense_comments.expense_id
      and public.is_active_poi_expense_member(expense.group_id)
));
create policy "expense_comments_member_create" on public.event_expense_comments
for insert to authenticated with check (author_id = auth.uid());
create policy "expense_comments_author_delete" on public.event_expense_comments
for delete to authenticated using (author_id = auth.uid() or public.is_poi_admin());

create policy "expense_settlements_member_read" on public.event_expense_settlements
for select to authenticated using (public.is_active_poi_expense_member(group_id));
create policy "expense_settlements_payer_create" on public.event_expense_settlements
for insert to authenticated with check (payer_id = auth.uid());
create policy "expense_settlements_payee_confirm" on public.event_expense_settlements
for update to authenticated using (payee_id = auth.uid() and status = 'pending')
with check (payee_id = auth.uid());
create policy "expense_activity_member_read" on public.event_expense_activity
for select to authenticated using (public.is_active_poi_expense_member(group_id));

revoke all on public.event_expense_groups, public.event_expense_members,
    public.event_expenses, public.event_expense_payments, public.event_expense_shares,
    public.event_expense_comments, public.event_expense_settlements,
    public.event_expense_activity from anon, authenticated;
grant select, insert on public.event_expense_groups to authenticated;
grant update (currency, updated_at_millis) on public.event_expense_groups to authenticated;
grant select, insert on public.event_expense_members to authenticated;
grant update (status, upi_id, updated_at_millis) on public.event_expense_members to authenticated;
grant select on public.event_expenses, public.event_expense_payments,
    public.event_expense_shares, public.event_expense_activity to authenticated;
grant select, insert, delete on public.event_expense_comments to authenticated;
grant select, insert on public.event_expense_settlements to authenticated;
grant update (status, confirmed_at_millis) on public.event_expense_settlements to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
    'event-expense-receipts', 'event-expense-receipts', false, 6291456,
    array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update set
    public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

create or replace function public.poi_expense_storage_group_id(object_name text)
returns uuid
language plpgsql
immutable
security definer set search_path = ''
as $$
begin
    return ((storage.foldername(object_name))[1])::uuid;
exception when others then
    return null;
end;
$$;

revoke all on function public.poi_expense_storage_group_id(text) from public;
grant execute on function public.poi_expense_storage_group_id(text) to authenticated;

create policy "event_expense_receipts_member_read"
on storage.objects for select to authenticated
using (
    bucket_id = 'event-expense-receipts'
    and public.is_active_poi_expense_member(public.poi_expense_storage_group_id(name))
);
create policy "event_expense_receipts_member_create"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'event-expense-receipts'
    and (storage.foldername(name))[2] = auth.uid()::text
    and public.is_active_poi_expense_member(public.poi_expense_storage_group_id(name))
);
create policy "event_expense_receipts_owner_delete"
on storage.objects for delete to authenticated
using (
    bucket_id = 'event-expense-receipts'
    and (owner_id = auth.uid()::text or public.is_poi_admin())
);

do $$
declare table_name text;
begin
    foreach table_name in array array[
        'event_expense_groups', 'event_expense_members', 'event_expenses',
        'event_expense_payments', 'event_expense_shares', 'event_expense_comments',
        'event_expense_settlements', 'event_expense_activity'
    ] loop
        if not exists (
            select 1 from pg_publication_tables
            where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = table_name
        ) then
            execute format('alter publication supabase_realtime add table public.%I', table_name);
        end if;
    end loop;
end $$;

commit;
