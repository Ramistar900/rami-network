create table if not exists rami_storage.hls_packages (
  id uuid primary key default gen_random_uuid(),
  asset_id uuid not null references rami_storage.media_assets(id) on delete cascade,
  channel_id uuid not null references rami_storage.channels(id),
  source_file_id text not null,
  drive_folder_id text,
  playlist_file_id text,
  playlist_name text,
  segments jsonb not null default '[]'::jsonb,
  duration_seconds numeric,
  segment_count integer not null default 0,
  codec text not null default 'h264/aac',
  status text not null check (status in ('processing', 'packaged', 'failed')),
  run_id text not null,
  run_attempt integer not null default 1,
  error_code text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists hls_packages_asset_status_created_idx
  on rami_storage.hls_packages (asset_id, status, created_at desc);
create index if not exists hls_packages_channel_status_created_idx
  on rami_storage.hls_packages (channel_id, status, created_at desc);
revoke all on table rami_storage.hls_packages from anon, authenticated;
grant select, insert, update on table rami_storage.hls_packages to service_role;
