create extension if not exists pg_cron with schema pg_catalog;

alter table rami_network.playback_states
  add column if not exists active_schedule_block_id uuid;

create or replace function public.rami_network_process_schedule()
returns jsonb
language plpgsql
set search_path = ''
as $function$
declare
  v_now timestamptz := clock_timestamp();
  v_channel record;
  v_block rami_network.schedule_blocks%rowtype;
  v_target_mode text;
  v_stream_url text;
  v_media_item_id uuid;
  v_changed integer := 0;
  v_processed integer := 0;
begin
  if not pg_try_advisory_xact_lock(hashtextextended('rami_network.schedule_engine', 0)) then
    return jsonb_build_object('ok', true, 'skipped', 'already_running', 'serverTime', v_now);
  end if;

  for v_channel in
    select c.id, c.is_enabled, c.archived_at, p.active_schedule_block_id
    from rami_network.channels c
    join rami_network.playback_states p on p.channel_id = c.id
    order by c.channel_number
    for update of p
  loop
    v_processed := v_processed + 1;
    v_target_mode := null;
    v_stream_url := null;
    v_media_item_id := null;

    if v_channel.is_enabled and v_channel.archived_at is null then
      select b.*
      into v_block
      from rami_network.schedule_blocks b
      where b.channel_id = v_channel.id
        and b.is_enabled = true
        and b.starts_at <= v_now
        and b.ends_at > v_now
      order by b.priority desc, b.starts_at desc, b.id
      limit 1;
    else
      v_block := null;
    end if;

    if v_block.id is not null then
      if v_block.block_type = 'playlist' then
        v_target_mode := 'playlist';
      elsif v_block.block_type = 'live_source' then
        select s.viewer_url
        into v_stream_url
        from rami_network.content_sources s
        where s.id = v_block.source_id
          and s.is_enabled = true
          and s.rights_status = 'approved'
          and s.viewer_url is not null;
        if found then
          v_target_mode := 'live';
        else
          v_target_mode := 'stopped';
        end if;
      elsif v_block.block_type = 'media_item' then
        select m.id, s.viewer_url
        into v_media_item_id, v_stream_url
        from rami_network.media_items m
        join rami_network.content_sources s on s.id = m.source_id
        where m.id = v_block.media_item_id
          and m.is_enabled = true
          and s.is_enabled = true
          and s.rights_status = 'approved'
          and s.viewer_url is not null;
        if found then
          v_target_mode := 'scheduled';
        else
          v_target_mode := 'stopped';
          v_media_item_id := null;
        end if;
      end if;

      update rami_network.playback_states p
      set active_schedule_block_id = v_block.id,
          playback_mode = v_target_mode,
          anchor_at = v_block.starts_at,
          stream_url = case when v_target_mode = 'live' then v_stream_url else null end,
          current_media_item_id = case when v_target_mode = 'scheduled' then v_media_item_id else null end,
          current_position_ms = 0,
          sequence = p.sequence + 1,
          schedule_version = p.schedule_version + 1,
          server_updated_at = v_now
      where p.channel_id = v_channel.id
        and (
          p.active_schedule_block_id is distinct from v_block.id
          or p.playback_mode is distinct from v_target_mode
          or p.anchor_at is distinct from v_block.starts_at
          or p.stream_url is distinct from case when v_target_mode = 'live' then v_stream_url else null end
          or p.current_media_item_id is distinct from case when v_target_mode = 'scheduled' then v_media_item_id else null end
        );

      if found then
        v_changed := v_changed + 1;
        insert into rami_network.audit_events(actor_user_id, action, entity_type, entity_id, details)
        values (null, 'schedule.block_activated', 'channel', v_channel.id,
          jsonb_build_object('schedule_block_id', v_block.id, 'block_type', v_block.block_type,
            'playback_mode', v_target_mode, 'scheduled_at', v_now));
      end if;
    elsif v_channel.active_schedule_block_id is not null then
      update rami_network.playback_states p
      set active_schedule_block_id = null,
          playback_mode = 'stopped',
          anchor_at = null,
          stream_url = null,
          current_media_item_id = null,
          current_position_ms = 0,
          sequence = p.sequence + 1,
          schedule_version = p.schedule_version + 1,
          server_updated_at = v_now
      where p.channel_id = v_channel.id
        and p.active_schedule_block_id is not null;

      if found then
        v_changed := v_changed + 1;
        insert into rami_network.audit_events(actor_user_id, action, entity_type, entity_id, details)
        values (null, 'schedule.block_finished', 'channel', v_channel.id,
          jsonb_build_object('previous_schedule_block_id', v_channel.active_schedule_block_id,
            'scheduled_at', v_now));
      end if;
    end if;
  end loop;

  return jsonb_build_object('ok', true, 'processedChannels', v_processed,
    'changedChannels', v_changed, 'serverTime', v_now);
end;
$function$;

create or replace function public.rami_network_scheduled_media_rpc(p_channel_id uuid)
returns jsonb
language plpgsql
set search_path = ''
as $function$
declare
  v_state rami_network.playback_states%rowtype;
  v_block rami_network.schedule_blocks%rowtype;
  v_title text;
  v_url text;
  v_source_type text;
  v_duration bigint;
  v_position bigint;
  v_now timestamptz := clock_timestamp();
begin
  select p.* into v_state
  from rami_network.playback_states p
  join rami_network.channels c on c.id = p.channel_id
  where p.channel_id = p_channel_id
    and c.is_enabled = true
    and c.archived_at is null
    and p.playback_mode = 'scheduled';

  if not found or v_state.active_schedule_block_id is null then
    return jsonb_build_object('channelId', p_channel_id, 'status', 'waiting_for_schedule',
      'playbackMode', 'scheduled', 'serverTime', v_now);
  end if;

  select b.* into v_block
  from rami_network.schedule_blocks b
  where b.id = v_state.active_schedule_block_id
    and b.is_enabled = true
    and b.starts_at <= v_now
    and b.ends_at > v_now;

  if not found then
    return jsonb_build_object('channelId', p_channel_id, 'status', 'waiting_for_schedule',
      'playbackMode', 'scheduled', 'sequence', v_state.sequence, 'serverTime', v_now);
  end if;

  select m.title, s.viewer_url, s.source_type, m.duration_ms
  into v_title, v_url, v_source_type, v_duration
  from rami_network.media_items m
  join rami_network.content_sources s on s.id = m.source_id
  where m.id = v_state.current_media_item_id
    and m.is_enabled = true
    and s.is_enabled = true
    and s.rights_status = 'approved'
    and s.viewer_url is not null;

  if not found or v_duration is null or v_duration <= 0 then
    return jsonb_build_object('channelId', p_channel_id, 'status', 'waiting_for_media',
      'playbackMode', 'scheduled', 'sequence', v_state.sequence, 'serverTime', v_now);
  end if;

  v_position := greatest(0, floor(extract(epoch from (v_now - v_state.anchor_at)) * 1000)::bigint);

  return jsonb_build_object(
    'channelId', p_channel_id,
    'sequence', v_state.sequence,
    'serverTime', v_now,
    'status', case when v_position < v_duration then 'playing' else 'waiting_for_schedule' end,
    'playbackMode', 'scheduled',
    'startedAt', v_block.starts_at,
    'endsAt', v_block.ends_at,
    'currentItem', case when v_position < v_duration then jsonb_build_object(
      'id', v_state.current_media_item_id,
      'title', v_title,
      'durationMs', v_duration,
      'positionMs', v_position,
      'mediaUrl', v_url,
      'sourceType', v_source_type
    ) else null end
  );
end;
$function$;

revoke all on function public.rami_network_process_schedule() from public, anon, authenticated;
revoke all on function public.rami_network_scheduled_media_rpc(uuid) from public, anon, authenticated;
grant execute on function public.rami_network_process_schedule() to service_role;
grant execute on function public.rami_network_scheduled_media_rpc(uuid) to service_role;

select cron.unschedule(jobid)
from cron.job
where jobname = 'rami-network-schedule-engine';

select cron.schedule(
  'rami-network-schedule-engine',
  '* * * * *',
  'select public.rami_network_process_schedule();'
);
