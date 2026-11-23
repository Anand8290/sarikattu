-- All writes. Each function checks auth.uid() and group membership itself.

-- Groups the caller belongs to, with their own balance in each.
create function public.my_groups()
returns table (id uuid, name text, currency text, member_count int, my_balance bigint)
language sql stable security definer set search_path = public as $$
    select g.id, g.name, g.currency,
           (select count(*)::int from members m where m.group_id = g.id),
           coalesce((
               select sum(e.amount_minor) from expenses e
               join members me on me.id = e.payer_id
               where e.group_id = g.id and e.deleted_at is null and me.claimed_by = auth.uid()
           ), 0)::bigint
           - coalesce((
               select sum(s.amount_minor) from expense_splits s
               join expenses e on e.id = s.expense_id
               join members me on me.id = s.member_id
               where e.group_id = g.id and e.deleted_at is null and me.claimed_by = auth.uid()
           ), 0)::bigint
    from groups g
    join group_access a on a.group_id = g.id and a.user_id = auth.uid()
    order by g.created_at desc;
$$;

-- Creates a group with its members; the caller becomes member_names[my_index] (0-based).
create function public.create_group(p_name text, p_currency text, p_member_names text[], p_my_index int)
returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_group uuid;
    i int;
begin
    if auth.uid() is null then raise exception 'not signed in'; end if;
    if coalesce(array_length(p_member_names, 1), 0) = 0 then raise exception 'add at least one member'; end if;
    if p_my_index < 0 or p_my_index >= array_length(p_member_names, 1) then raise exception 'bad member index'; end if;

    insert into groups (name, currency) values (trim(p_name), upper(p_currency)) returning id into v_group;
    insert into group_access (user_id, group_id) values (auth.uid(), v_group);
    for i in 1 .. array_length(p_member_names, 1) loop
        insert into members (group_id, name, claimed_by)
        values (v_group, trim(p_member_names[i]), case when i - 1 = p_my_index then auth.uid() end);
    end loop;
    return v_group;
end $$;

-- Lets someone with an invite code see the group name and who has already joined.
create function public.group_preview(p_code text)
returns jsonb
language sql stable security definer set search_path = public as $$
    select jsonb_build_object(
        'id', g.id,
        'name', g.name,
        'members', coalesce((
            select jsonb_agg(jsonb_build_object('id', m.id, 'name', m.name, 'claimed', m.claimed_by is not null)
                             order by m.created_at)
            from members m where m.group_id = g.id
        ), '[]'::jsonb)
    )
    from groups g where g.invite_code = p_code;
$$;

-- Joins a group as an existing unclaimed member, or as a new member named p_new_name.
create function public.join_group(p_code text, p_member_id uuid, p_new_name text)
returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_group uuid;
begin
    if auth.uid() is null then raise exception 'not signed in'; end if;
    select id into v_group from groups where invite_code = p_code;
    if v_group is null then raise exception 'invite code not found'; end if;

    if exists (select 1 from members where group_id = v_group and claimed_by = auth.uid()) then
        return v_group; -- already joined
    end if;

    if p_member_id is not null then
        update members set claimed_by = auth.uid()
        where id = p_member_id and group_id = v_group and claimed_by is null;
        if not found then raise exception 'that member has already been claimed'; end if;
    else
        if coalesce(trim(p_new_name), '') = '' then raise exception 'enter your name'; end if;
        insert into members (group_id, name, claimed_by) values (v_group, trim(p_new_name), auth.uid());
    end if;

    insert into group_access (user_id, group_id) values (auth.uid(), v_group) on conflict do nothing;
    return v_group;
end $$;

-- Adds or edits an expense. p_splits is [{"member_id": "...", "amount_minor": 123}, ...].
create function public.save_expense(
    p_id uuid, p_group_id uuid, p_title text, p_amount bigint, p_payer uuid,
    p_is_settlement boolean, p_spent_on date, p_splits jsonb)
returns uuid
language plpgsql security definer set search_path = public as $$
declare
    v_id  uuid := coalesce(p_id, gen_random_uuid());
    v_sum bigint;
begin
    if not is_member(p_group_id) then raise exception 'not a member of this group'; end if;
    if not exists (select 1 from members where id = p_payer and group_id = p_group_id) then
        raise exception 'payer is not in this group';
    end if;
    if exists (
        select 1 from jsonb_array_elements(p_splits) s
        where not exists (select 1 from members m
                          where m.id = (s ->> 'member_id')::uuid and m.group_id = p_group_id)
    ) then
        raise exception 'split member is not in this group';
    end if;
    select coalesce(sum((s ->> 'amount_minor')::bigint), 0) into v_sum from jsonb_array_elements(p_splits) s;
    if v_sum <> p_amount then raise exception 'splits must add up to the amount'; end if;

    if p_id is not null and exists (select 1 from expenses where id = p_id and group_id <> p_group_id) then
        raise exception 'expense belongs to another group';
    end if;

    insert into expenses (id, group_id, title, amount_minor, payer_id, is_settlement, spent_on)
    values (v_id, p_group_id, trim(p_title), p_amount, p_payer, p_is_settlement, p_spent_on)
    on conflict (id) do update set
        title = excluded.title,
        amount_minor = excluded.amount_minor,
        payer_id = excluded.payer_id,
        is_settlement = excluded.is_settlement,
        spent_on = excluded.spent_on,
        updated_at = now();

    delete from expense_splits where expense_id = v_id;
    insert into expense_splits (expense_id, member_id, amount_minor)
    select v_id, (s ->> 'member_id')::uuid, (s ->> 'amount_minor')::bigint
    from jsonb_array_elements(p_splits) s;
    return v_id;
end $$;

-- Soft delete, so a mistaken delete can be undone later.
create function public.delete_expense(p_id uuid)
returns void
language plpgsql security definer set search_path = public as $$
begin
    update expenses set deleted_at = now(), updated_at = now()
    where id = p_id and is_member(group_id) and deleted_at is null;
end $$;

-- Leaves a group: the caller's member stays (expenses keep their history) but is unclaimed.
-- A group nobody can access any more is deleted.
create function public.leave_group(p_group_id uuid)
returns void
language plpgsql security definer set search_path = public as $$
begin
    update members set claimed_by = null where group_id = p_group_id and claimed_by = auth.uid();
    delete from group_access where group_id = p_group_id and user_id = auth.uid();
    delete from groups g where g.id = p_group_id
        and not exists (select 1 from group_access a where a.group_id = g.id);
end $$;

-- Play Store data deletion: leaves every group and deletes the anonymous account.
create function public.delete_my_data()
returns void
language plpgsql security definer set search_path = public, auth as $$
declare
    r record;
begin
    if auth.uid() is null then return; end if;
    for r in select group_id from public.group_access where user_id = auth.uid() loop
        perform public.leave_group(r.group_id);
    end loop;
    delete from auth.users where id = auth.uid();
end $$;

revoke execute on all functions in schema public from public, anon;
grant execute on function
    public.my_groups(), public.create_group(text, text, text[], int), public.group_preview(text),
    public.join_group(text, uuid, text), public.save_expense(uuid, uuid, text, bigint, uuid, boolean, date, jsonb),
    public.delete_expense(uuid), public.leave_group(uuid), public.delete_my_data(), public.is_member(uuid)
    to authenticated;
