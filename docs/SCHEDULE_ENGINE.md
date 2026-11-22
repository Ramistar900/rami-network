# RAMİ NETWORK schedule engine

## Runtime
- Database migration: `20261122023456_rami_network_schedule_engine`.
- Supabase Cron job: `rami-network-schedule-engine`.
- Interval: every minute.
- The job invokes `public.rami_network_process_schedule()` inside Postgres; it does not call an external worker or video provider.
- A transaction-scoped advisory lock prevents overlapping scheduler executions.

## Schedule block behavior
- `playlist`: activates the channel's existing playlist using the block's `starts_at` as the shared playback anchor.
- `live_source`: activates the approved source's public `viewer_url` as the channel's live HLS output. Private `source_url` is never exposed.
- `media_item`: activates one approved media item and returns its public URL and calculated playback position through `rami_network_scheduled_media_rpc`.

When blocks overlap, the block with the greatest `priority` wins. If priorities match, the later `starts_at` wins, followed by a stable ID tie-break. A block is active when `starts_at <= server_time < ends_at`.

When the active block changes, the playback sequence and schedule version increment. If no block remains after a scheduled block, the channel is stopped. If the selected source or media item is not approved and publicly playable, the channel remains stopped rather than exposing a private origin.

## Operations and limits
- Job history is available in `cron.job_run_details`.
- Keep job work short; the function only reads schedule rows and updates channel state.
- Cron uses the existing Supabase Postgres instance and its included plan resources. It still consumes database compute and storage quotas; monitor usage and do not enable billing or paid compute.
- This engine coordinates playback state only. It does not ingest, transcode, package, store, or distribute video. Actual continuous live channels still require a separately authorized HLS output source.
