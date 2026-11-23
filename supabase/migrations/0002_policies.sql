-- Access rules. Clients may only READ tables, and only for groups they have joined.
-- Every write goes through the SECURITY DEFINER functions in 0003_functions.sql,
-- which check membership and keep the data consistent.

create function public.is_member(gid uuid) returns boolean
language sql stable security definer set search_path = public as $$
    select exists (
        select 1 from public.group_access
        where group_id = gid and user_id = auth.uid()
    );
$$;

alter table public.groups         enable row level security;
alter table public.members        enable row level security;
alter table public.expenses       enable row level security;
alter table public.expense_splits enable row level security;
alter table public.group_access   enable row level security;

create policy "members read groups" on public.groups
    for select to authenticated using (public.is_member(id));

create policy "members read members" on public.members
    for select to authenticated using (public.is_member(group_id));

create policy "members read expenses" on public.expenses
    for select to authenticated using (public.is_member(group_id));

create policy "members read splits" on public.expense_splits
    for select to authenticated using (
        exists (select 1 from public.expenses e
                where e.id = expense_id and public.is_member(e.group_id))
    );

create policy "users read own access" on public.group_access
    for select to authenticated using (user_id = auth.uid());

-- No direct writes from the app.
revoke insert, update, delete on all tables in schema public from anon, authenticated;
revoke all on all tables in schema public from anon;
