create or replace function public.rami_network_create_schedule_block(p_payload jsonb)
returns jsonb
language plpgsql
set search_path = ''
as $function$
declare
  v_channel_id uuid;
  v_source_id uuid;
  v_media_item_id uuid;
  v_actor uuid;
  v_block rami_network.schedule_blocks%rowtype;
begin
  if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
    raise exception 'payload_must_be_object' using errcode = '22023';
  end if;

  v_channel_id := nullif(p_payload->>'channel_id','')::uuid;
  v_source_id := nullif(p_payload->>'source_id','')::uuid;
  v_media_item_id := nullif(p_payload->>'media_item_id','')::uuid;
  v_actor := nullif(p_payload->>'actor_user_id','')::uuid;

  if v_channel_id is null then
    raise exception 'channel_id_required' using errcode = '22023';
  end if;

  perform 1 from rami_network.channels c
  where c.id = v_channel_id and c.archived_at is null
  for update;
  if not found then raise exception 'channel_not_found'; end if;

  insert into rami_network.schedule_blocks(
    channel_id,block_type,starts_at,ends_at,source_id,media_item_id,priority
  )
  values (
    v_channel_id,
    p_payload->>'block_type',
    (p_payload->>'starts_at')::timestamptz,
    (p_payload->>'ends_at')::timestamptz,
    v_source_id,
    v_media_item_id,
    coalesce(nullif(p_payload->>'priority','')::integer,100)
  )
  returning * into v_block;

  insert into rami_network.audit_events(actor_user_id,action,entity_type,entity_id,details)
  values (v_actor,'schedule.block_created','channel',v_channel_id,
    jsonb_build_object('schedule_block_id',v_block.id,'block_type',v_block.block_type));

  return to_jsonb(v_block);
end;
$function$;

revoke all on function public.rami_network_create_schedule_block(jsonb) from public, anon, authenticated;
grant execute on function public.rami_network_create_schedule_block(jsonb) to service_role;
