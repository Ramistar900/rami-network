create table if not exists rami_storage.owner_password_bootstrap (
  singleton boolean primary key default true check (singleton = true),
  claimed_at timestamptz,
  claimed_by uuid,
  consumed_at timestamptz,
  consumed_by uuid
);

insert into rami_storage.owner_password_bootstrap
  (singleton, claimed_at, claimed_by, consumed_at, consumed_by)
values (true, null, null, null, null)
on conflict (singleton) do nothing;

alter table rami_storage.owner_password_bootstrap enable row level security;

revoke all on table rami_storage.owner_password_bootstrap from public, anon, authenticated;
grant select, update on table rami_storage.owner_password_bootstrap to service_role;
