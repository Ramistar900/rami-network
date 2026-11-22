create or replace function rami_network.reindex_playlist_positions(p_channel_id uuid)
returns void
language plpgsql
set search_path = ''
as $function$
declare
  v_count integer;
  v_max integer;
  v_offset integer;
begin
  select count(*)::integer, coalesce(max(e.position), 0)
  into v_count, v_max
  from rami_network.playlist_entries e
  where e.channel_id = p_channel_id;

  if v_count = 0 then return; end if;
  if v_max::bigint + v_count::bigint + 1 > 2147483647 then
    raise exception 'playlist_position_limit_exceeded' using errcode = '22003';
  end if;

  v_offset := v_max + v_count + 1;

  update rami_network.playlist_entries e
  set position = e.position + v_offset
  where e.channel_id = p_channel_id;

  with ranked as (
    select e.id, row_number() over (order by e.position, e.id)::integer as new_position
    from rami_network.playlist_entries e
    where e.channel_id = p_channel_id
  )
  update rami_network.playlist_entries e
  set position = ranked.new_position
  from ranked
  where e.id = ranked.id;
end;
$function$;

create or replace function public.rami_network_schedule_admin_rpc(
  p_action text,
  p_payload jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $function$
declare
  v_block_id uuid;
  v_channel_id uuid;
  v_actor uuid;
  v_result jsonb;
begin
  if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
    raise exception 'payload_must_be_object' using errcode = '22023';
  end if;

  v_block_id := nullif(p_payload->>'block_id','')::uuid;
  v_channel_id := nullif(p_payload->>'channel_id','')::uuid;
  v_actor := nullif(p_payload->>'actor_user_id','')::uuid;

  if v_block_id is null or v_channel_id is null then
    raise exception 'block_id_and_channel_id_required' using errcode = '22023';
  end if;

  if p_action = 'update_schedule_block' then
    update rami_network.schedule_blocks b
    set starts_at = case when p_payload ? 'starts_at' then (p_payload->>'starts_at')::timestamptz else b.starts_at end,
        ends_at = case when p_payload ? 'ends_at' then (p_payload->>'ends_at')::timestamptz else b.ends_at end,
        priority = case when p_payload ? 'priority' then (p_payload->>'priority')::integer else b.priority end,
        is_enabled = case when p_payload ? 'is_enabled' then (p_payload->>'is_enabled')::boolean else b.is_enabled end
    where b.id = v_block_id and b.channel_id = v_channel_id
    returning to_jsonb(b) into v_result;

    if v_result is null then raise exception 'schedule_block_not_found'; end if;

    insert into rami_network.audit_events(actor_user_id,action,entity_type,entity_id,details)
    values (v_actor,'schedule.block_updated','schedule_block',v_block_id,
      jsonb_build_object('channel_id',v_channel_id));
    return v_result;

  elsif p_action = 'delete_schedule_block' then
    delete from rami_network.schedule_blocks b
    where b.id = v_block_id and b.channel_id = v_channel_id
    returning to_jsonb(b) into v_result;

    if v_result is null then raise exception 'schedule_block_not_found'; end if;

    insert into rami_network.audit_events(actor_user_id,action,entity_type,entity_id,details)
    values (v_actor,'schedule.block_deleted','schedule_block',v_block_id,
      jsonb_build_object('channel_id',v_channel_id));
    return jsonb_build_object('ok',true,'deleted',v_result);
  end if;

  raise exception 'unsupported_schedule_admin_action' using errcode = '22023';
end;
$function$;

create or replace function public.rami_network_playlist_admin_rpc(
  p_action text,
  p_payload jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $function$
declare
  v_channel_id uuid;
  v_entry_id uuid;
  v_actor uuid;
  v_count integer;
  v_payload_count integer;
  v_distinct_count integer;
  v_max integer;
  v_offset integer;
  v_result jsonb;
begin
  if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
    raise exception 'payload_must_be_object' using errcode = '22023';
  end if;

  v_channel_id := nullif(p_payload->>'channel_id','')::uuid;
  v_actor := nullif(p_payload->>'actor_user_id','')::uuid;
  if v_channel_id is null then
    raise exception 'channel_id_required' using errcode = '22023';
  end if;

  perform 1 from rami_network.channels c
  where c.id = v_channel_id and c.archived_at is null
  for update;
  if not found then raise exception 'channel_not_found'; end if;

  if p_action = 'remove_playlist_entry' then
    v_entry_id := nullif(p_payload->>'entry_id','')::uuid;

    delete from rami_network.playlist_entries e
    where e.id = v_entry_id and e.channel_id = v_channel_id;
    if not found then raise exception 'playlist_entry_not_found'; end if;

    perform rami_network.reindex_playlist_positions(v_channel_id);

    insert into rami_network.audit_events(actor_user_id,action,entity_type,entity_id,details)
    values (v_actor,'playlist.entry_removed','channel',v_channel_id,
      jsonb_build_object('entry_id',v_entry_id));

  elsif p_action = 'reorder_playlist_entries' then
    if jsonb_typeof(p_payload->'entry_ids') <> 'array' then
      raise exception 'entry_ids_must_be_array' using errcode = '22023';
    end if;

    if exists (
      select 1 from jsonb_array_elements_text(p_payload->'entry_ids') as x(value)
      where x.value !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
    ) then
      raise exception 'invalid_playlist_entry_id' using errcode = '22023';
    end if;

    select count(*)::integer, count(distinct x.value)::integer
    into v_payload_count, v_distinct_count
    from jsonb_array_elements_text(p_payload->'entry_ids') as x(value);

    select count(*)::integer, coalesce(max(e.position),0)
    into v_count, v_max
    from rami_network.playlist_entries e
    where e.channel_id = v_channel_id;

    if v_payload_count <> v_count or v_distinct_count <> v_count then
      raise exception 'reorder_must_include_every_playlist_entry_once' using errcode = '22023';
    end if;

    if exists (
      select 1
      from jsonb_array_elements_text(p_payload->'entry_ids') as x(value)
      where not exists (
        select 1 from rami_network.playlist_entries e
        where e.channel_id = v_channel_id and e.id = x.value::uuid
      )
    ) then
      raise exception 'playlist_entry_not_in_channel' using errcode = '22023';
    end if;

    if v_count > 0 then
      if v_max::bigint + v_count::bigint + 1 > 2147483647 then
        raise exception 'playlist_position_limit_exceeded' using errcode = '22003';
      end if;

      v_offset := v_max + v_count + 1;
      update rami_network.playlist_entries e
      set position = e.position + v_offset
      where e.channel_id = v_channel_id;

      with ordered as (
        select x.value::uuid as id, x.ordinality::integer as new_position
        from jsonb_array_elements_text(p_payload->'entry_ids') with ordinality as x(value, ordinality)
      )
      update rami_network.playlist_entries e
      set position = ordered.new_position
      from ordered
      where e.id = ordered.id and e.channel_id = v_channel_id;
    end if;

    insert into rami_network.audit_events(actor_user_id,action,entity_type,entity_id,details)
    values (v_actor,'playlist.reordered','channel',v_channel_id,
      jsonb_build_object('entry_count',v_count));
  else
    raise exception 'unsupported_playlist_admin_action' using errcode = '22023';
  end if;

  select coalesce(jsonb_agg(
    jsonb_build_object('id',e.id,'channel_id',e.channel_id,'media_item_id',e.media_item_id,
      'position',e.position,'is_enabled',e.is_enabled,'media',to_jsonb(m))
    order by e.position
  ),'[]'::jsonb)
  into v_result
  from rami_network.playlist_entries e
  join rami_network.media_items m on m.id=e.media_item_id
  where e.channel_id = v_channel_id;

  return v_result;
end;
$function$;

revoke all on function rami_network.reindex_playlist_positions(uuid) from public, anon, authenticated;
revoke all on function public.rami_network_schedule_admin_rpc(text,jsonb) from public, anon, authenticated;
revoke all on function public.rami_network_playlist_admin_rpc(text,jsonb) from public, anon, authenticated;
grant execute on function rami_network.reindex_playlist_positions(uuid) to service_role;
grant execute on function public.rami_network_schedule_admin_rpc(text,jsonb) to service_role;
grant execute on function public.rami_network_playlist_admin_rpc(text,jsonb) to service_role;
