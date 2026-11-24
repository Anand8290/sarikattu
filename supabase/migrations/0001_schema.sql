-- SariKattu schema. Money is stored as bigint minor units (paise, cents).

create table public.groups (
    id          uuid primary key default gen_random_uuid(),
    name        text not null check (char_length(name) between 1 and 80),
    currency    text not null check (currency ~ '^[A-Z]{3}$'),
    -- 12 hex chars from a random UUID: unguessable enough for invite links.
    invite_code text not null unique default substr(replace(gen_random_uuid()::text, '-', ''), 1, 12),
    created_at  timestamptz not null default now()
);

create table public.members (
    id         uuid primary key default gen_random_uuid(),
    group_id   uuid not null references public.groups (id) on delete cascade,
    name       text not null check (char_length(name) between 1 and 40),
    -- The anonymous auth user who said "this is me". Null until someone claims it.
    claimed_by uuid references auth.users (id) on delete set null,
    created_at timestamptz not null default now(),
    unique (group_id, claimed_by)
);
create index members_group_idx on public.members (group_id);

create table public.expenses (
    id            uuid primary key default gen_random_uuid(),
    group_id      uuid not null references public.groups (id) on delete cascade,
    title         text not null check (char_length(title) between 1 and 80),
    amount_minor  bigint not null check (amount_minor > 0),
    -- Members are only deleted along with their group, so cascading here is safe.
    payer_id      uuid not null references public.members (id) on delete cascade,
    is_settlement boolean not null default false,
    spent_on      date not null default current_date,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    deleted_at    timestamptz
);
create index expenses_group_idx on public.expenses (group_id);

create table public.expense_splits (
    expense_id   uuid not null references public.expenses (id) on delete cascade,
    member_id    uuid not null references public.members (id) on delete cascade,
    amount_minor bigint not null check (amount_minor >= 0),
    primary key (expense_id, member_id)
);

-- Which auth users can see which groups. Rows are only added by join_group/create_group.
create table public.group_access (
    user_id  uuid not null references auth.users (id) on delete cascade,
    group_id uuid not null references public.groups (id) on delete cascade,
    primary key (user_id, group_id)
);
create index group_access_group_idx on public.group_access (group_id);
