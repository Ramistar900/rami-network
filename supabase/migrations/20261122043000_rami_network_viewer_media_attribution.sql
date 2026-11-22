create or replace function public.rami_network_scheduled_media_rpc(p_channel_id uuid)
returns jsonb
language plpgsql
set search_path to ''
as $function$
declare
  v_state rami_network.playback_states%rowtype;
  v_block rami_network.schedule_blocks%rowtype;
  v_title text;
  v_url text;
  v_source_type text;
  v_duration bigint;
  v_description text;
  v_rights_note text;
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

  select m.title, s.viewer_url, s.source_type, m.duration_ms, m.description, s.rights_note
  into v_title, v_url, v_source_type, v_duration, v_description, v_rights_note
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
      'sourceType', v_source_type,
      'description', v_description,
      'rightsNote', v_rights_note
    ) else null end
  );
end;
$function$;

create or replace function public.rami_network_viewer_rpc(p_action text, p_channel_id uuid default null)
returns jsonb
language plpgsql
set search_path to ''
as $function$
declare
  v_result jsonb;
  v_channel rami_network.channels%rowtype;
  v_state rami_network.playback_states%rowtype;
  v_total bigint;
  v_elapsed bigint;
  v_offset bigint;
  v_cycle bigint;
  v_current jsonb;
  v_playlist jsonb;
  v_item record;
  v_before bigint := 0;
begin
  if p_action = 'channels' then
    select coalesce(jsonb_agg(jsonb_build_object(
      'id',c.id,'number',c.channel_number,'slug',c.slug,'name',c.name,
      'logoUrl',c.logo_url,'playbackMode',c.playback_mode,
      'stateUrl','/functions/v1/rami-network-viewer?action=state&channel_id=' || c.id::text
    ) order by c.channel_number),'[]'::jsonb)
    into v_result
    from rami_network.channels c
    where c.is_enabled=true and c.archived_at is null;
    return jsonb_build_object('version',1,'serverTime',now(),'channels',v_result);
  end if;

  if p_action <> 'state' or p_channel_id is null then
    raise exception 'invalid_viewer_request' using errcode='22023';
  end if;

  select * into v_channel from rami_network.channels c
  where c.id=p_channel_id and c.is_enabled=true and c.archived_at is null;
  if not found then return jsonb_build_object('error','channel_not_found'); end if;

  select * into v_state from rami_network.playback_states p where p.channel_id=p_channel_id;
  if not found then
    return jsonb_build_object('channelId',p_channel_id,'sequence',0,'serverTime',now(),
      'status','stopped','playbackMode','stopped');
  end if;

  if v_state.playback_mode='live' then
    return jsonb_build_object(
      'channelId',p_channel_id,'sequence',v_state.sequence,'serverTime',now(),
      'status',case when v_state.stream_url is null then 'waiting_for_stream' else 'playing' end,
      'playbackMode','live',
      'stream',case when v_state.stream_url is null then null else
        jsonb_build_object('url',v_state.stream_url,'format','hls') end,
      'startedAt',v_state.anchor_at
    );
  end if;

  if v_state.playback_mode='playlist' then
    select coalesce(sum(m.duration_ms),0) into v_total
    from rami_network.playlist_entries e
    join rami_network.media_items m on m.id=e.media_item_id and m.is_enabled=true
    join rami_network.content_sources s on s.id=m.source_id
      and s.is_enabled=true and s.rights_status='approved' and s.viewer_url is not null
    where e.channel_id=p_channel_id and e.is_enabled=true and m.duration_ms is not null;

    if v_total<=0 or v_state.anchor_at is null then
      return jsonb_build_object('channelId',p_channel_id,'sequence',v_state.sequence,
        'serverTime',now(),'status','waiting_for_playlist','playbackMode','playlist');
    end if;

    v_elapsed := greatest(0,(extract(epoch from (now()-v_state.anchor_at))*1000)::bigint);
    v_cycle := v_elapsed / v_total;
    v_offset := mod(v_elapsed,v_total);

    select coalesce(jsonb_agg(jsonb_build_object(
      'id',m.id,'title',m.title,'durationMs',m.duration_ms,
      'mediaUrl',s.viewer_url,'sourceType',s.source_type,'position',e.position,
      'description',m.description,'rightsNote',s.rights_note
    ) order by e.position),'[]'::jsonb)
    into v_playlist
    from rami_network.playlist_entries e
    join rami_network.media_items m on m.id=e.media_item_id and m.is_enabled=true
    join rami_network.content_sources s on s.id=m.source_id
      and s.is_enabled=true and s.rights_status='approved' and s.viewer_url is not null
    where e.channel_id=p_channel_id and e.is_enabled=true and m.duration_ms is not null;

    for v_item in
      select m.id,m.title,m.duration_ms,e.position,s.viewer_url,s.source_type,m.description,s.rights_note
      from rami_network.playlist_entries e
      join rami_network.media_items m on m.id=e.media_item_id and m.is_enabled=true
      join rami_network.content_sources s on s.id=m.source_id
        and s.is_enabled=true and s.rights_status='approved' and s.viewer_url is not null
      where e.channel_id=p_channel_id and e.is_enabled=true and m.duration_ms is not null
      order by e.position
    loop
      if v_offset < v_before + v_item.duration_ms then
        v_current := jsonb_build_object(
          'id',v_item.id,'title',v_item.title,'position',v_item.position,
          'positionMs',v_offset-v_before,'durationMs',v_item.duration_ms,
          'mediaUrl',v_item.viewer_url,'sourceType',v_item.source_type,
          'description',v_item.description,'rightsNote',v_item.rights_note
        );
        exit;
      end if;
      v_before := v_before + v_item.duration_ms;
    end loop;

    return jsonb_build_object(
      'channelId',p_channel_id,'sequence',v_state.sequence,'serverTime',now(),
      'status','playing','playbackMode','playlist','startedAt',v_state.anchor_at,
      'cycleNumber',v_cycle,'currentItem',v_current,'playlist',v_playlist,
      'positionMs',coalesce((v_current->>'positionMs')::bigint,0)
    );
  end if;

  return jsonb_build_object('channelId',p_channel_id,'sequence',v_state.sequence,
    'serverTime',now(),'status','stopped','playbackMode',v_state.playback_mode);
end;
$function$;
