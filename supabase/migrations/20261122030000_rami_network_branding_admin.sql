create or replace function public.rami_network_branding_admin_rpc(
  p_action text,
  p_payload jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
set search_path = ''
as $function$
declare
  v_settings jsonb;
  v_actor uuid;
  v_logo_url text;
  v_width integer;
  v_opacity integer;
  v_x integer;
  v_y integer;
  v_enabled boolean;
begin
  if p_payload is null or jsonb_typeof(p_payload) <> 'object' then
    raise exception 'payload_must_be_object' using errcode = '22023';
  end if;

  if p_action = 'get_branding' then
    select s.setting_value into v_settings
    from rami_network.network_settings s
    where s.setting_key = 'brand_overlay';

    return coalesce(v_settings, jsonb_build_object(
      'enabled', true, 'logo_url', '', 'width_dp', 96,
      'opacity', 100, 'position_x', 50, 'position_y', 8
    ));
  elsif p_action = 'save_branding' then
    v_actor := nullif(p_payload->>'actor_user_id','')::uuid;
    v_enabled := coalesce((p_payload->>'enabled')::boolean, true);
    v_logo_url := coalesce(p_payload->>'logo_url','');
    v_width := coalesce((p_payload->>'width_dp')::integer,96);
    v_opacity := coalesce((p_payload->>'opacity')::integer,100);
    v_x := coalesce((p_payload->>'position_x')::integer,50);
    v_y := coalesce((p_payload->>'position_y')::integer,8);

    if length(v_logo_url) > 2048 then
      raise exception 'logo_url_too_long' using errcode = '22023';
    end if;
    if v_logo_url <> '' and v_logo_url !~* '^https://[^[:space:]]+$' then
      raise exception 'logo_url_must_be_https' using errcode = '22023';
    end if;
    if v_width < 24 or v_width > 180 then
      raise exception 'logo_width_out_of_range' using errcode = '22023';
    end if;
    if v_opacity < 0 or v_opacity > 100 then
      raise exception 'logo_opacity_out_of_range' using errcode = '22023';
    end if;
    if v_x < 0 or v_x > 100 or v_y < 0 or v_y > 100 then
      raise exception 'logo_position_out_of_range' using errcode = '22023';
    end if;

    v_settings := jsonb_build_object(
      'enabled',v_enabled, 'logo_url',v_logo_url, 'width_dp',v_width,
      'opacity',v_opacity, 'position_x',v_x, 'position_y',v_y
    );

    insert into rami_network.network_settings(setting_key,setting_value,updated_at)
    values ('brand_overlay',v_settings,now())
    on conflict (setting_key) do update
      set setting_value = excluded.setting_value, updated_at = excluded.updated_at;

    insert into rami_network.audit_events(actor_user_id,action,entity_type,entity_id,details)
    values (v_actor,'branding.updated','network_settings',null,v_settings);

    return v_settings;
  end if;

  raise exception 'unsupported_branding_action' using errcode = '22023';
end;
$function$;

revoke all on function public.rami_network_branding_admin_rpc(text,jsonb) from public, anon, authenticated;
grant execute on function public.rami_network_branding_admin_rpc(text,jsonb) to service_role;
