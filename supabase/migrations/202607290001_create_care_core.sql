create table if not exists public.tenants (
  id uuid primary key default gen_random_uuid(),
  code text not null unique,
  name text not null,
  status text not null default 'active' check (status in ('active', 'suspended', 'closed')),
  plan text not null default 'standard',
  created_at timestamptz not null default now()
);

create table if not exists public.tenant_members (
  tenant_id uuid not null references public.tenants(id),
  user_id text not null,
  role_code text not null,
  data_scope text not null default 'tenant',
  created_at timestamptz not null default now(),
  primary key (tenant_id, user_id)
);

create table if not exists public.rooms (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id),
  building text not null,
  floor text not null,
  room_no text not null,
  room_type text not null,
  created_at timestamptz not null default now(),
  unique (tenant_id, room_no)
);

create table if not exists public.beds (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id),
  room_id uuid not null references public.rooms(id),
  bed_no text not null,
  occupancy_status text not null default 'available' check (occupancy_status in ('available', 'reserved', 'occupied', 'cleaning', 'maintenance')),
  hygiene_status text not null default 'ready' check (hygiene_status in ('ready', 'cleaning', 'blocked')),
  version integer not null default 1,
  created_at timestamptz not null default now(),
  unique (room_id, bed_no)
);

create table if not exists public.residents (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id),
  name text not null,
  gender text,
  birth_date date,
  status text not null default 'pending_admission' check (status in ('pending_admission', 'in_residence', 'away', 'discharged', 'deceased')),
  current_bed_id uuid references public.beds(id),
  emergency_contact jsonb not null default '{}'::jsonb,
  risk_summary jsonb not null default '{}'::jsonb,
  archived_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.care_plans (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id),
  resident_id uuid not null references public.residents(id),
  title text not null,
  frequency text not null,
  owner_user_id text not null,
  status text not null default 'draft' check (status in ('draft', 'active', 'paused', 'ended')),
  created_at timestamptz not null default now()
);

create table if not exists public.care_tasks (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id),
  plan_id uuid not null references public.care_plans(id),
  scheduled_at timestamptz not null,
  assignee_user_id text,
  status text not null default 'pending' check (status in ('pending', 'in_progress', 'pending_handover', 'completed', 'overdue', 'cancelled')),
  exception_note text,
  completed_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.devices (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id),
  device_code text not null,
  device_type text not null,
  location text not null,
  online_status text not null default 'offline',
  battery_percent numeric(5, 2),
  last_heartbeat_at timestamptz,
  created_at timestamptz not null default now(),
  unique (tenant_id, device_code)
);

create table if not exists public.device_alerts (
  id uuid primary key default gen_random_uuid(),
  tenant_id uuid not null references public.tenants(id),
  device_id uuid not null references public.devices(id),
  priority text not null check (priority in ('low', 'medium', 'high', 'critical')),
  status text not null default 'pending' check (status in ('pending', 'acknowledged', 'dispatched', 'processing', 'pending_recheck', 'closed')),
  acknowledged_by text,
  acknowledged_at timestamptz,
  closed_at timestamptz,
  outcome text,
  created_at timestamptz not null default now()
);

create index if not exists residents_tenant_status_idx on public.residents (tenant_id, status) where archived_at is null;
create index if not exists care_tasks_tenant_schedule_idx on public.care_tasks (tenant_id, scheduled_at, status);
create index if not exists device_alerts_tenant_queue_idx on public.device_alerts (tenant_id, status, priority, created_at desc);

alter table public.tenants enable row level security;
alter table public.tenant_members enable row level security;
alter table public.rooms enable row level security;
alter table public.beds enable row level security;
alter table public.residents enable row level security;
alter table public.care_plans enable row level security;
alter table public.care_tasks enable row level security;
alter table public.devices enable row level security;
alter table public.device_alerts enable row level security;

create or replace function public.current_tenant_id()
returns uuid
language sql
stable
as $$
  select nullif(auth.jwt() ->> 'tenant_id', '')::uuid
$$;

create policy "tenant isolation" on public.residents
  using (tenant_id = public.current_tenant_id())
  with check (tenant_id = public.current_tenant_id());
create policy "tenant isolation" on public.tenants
  using (id = public.current_tenant_id());
create policy "tenant isolation" on public.tenant_members
  using (tenant_id = public.current_tenant_id())
  with check (tenant_id = public.current_tenant_id());
create policy "tenant isolation" on public.rooms
  using (tenant_id = public.current_tenant_id())
  with check (tenant_id = public.current_tenant_id());
create policy "tenant isolation" on public.beds
  using (tenant_id = public.current_tenant_id())
  with check (tenant_id = public.current_tenant_id());
create policy "tenant isolation" on public.care_plans
  using (tenant_id = public.current_tenant_id())
  with check (tenant_id = public.current_tenant_id());
create policy "tenant isolation" on public.care_tasks
  using (tenant_id = public.current_tenant_id())
  with check (tenant_id = public.current_tenant_id());
create policy "tenant isolation" on public.devices
  using (tenant_id = public.current_tenant_id())
  with check (tenant_id = public.current_tenant_id());
create policy "tenant isolation" on public.device_alerts
  using (tenant_id = public.current_tenant_id())
  with check (tenant_id = public.current_tenant_id());
