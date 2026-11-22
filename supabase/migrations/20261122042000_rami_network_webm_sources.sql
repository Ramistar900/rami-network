alter table rami_network.content_sources
  drop constraint if exists content_sources_source_type_check;

alter table rami_network.content_sources
  add constraint content_sources_source_type_check
  check (source_type = any (array['hls'::text, 'mp4'::text, 'webm'::text, 'rtmp'::text, 'srt'::text]));

alter table rami_network.content_sources
  drop constraint if exists content_sources_secure_url_scheme_check;

alter table rami_network.content_sources
  add constraint content_sources_secure_url_scheme_check
  check (
    source_url is null
    or (
      source_type = any (array['hls'::text, 'mp4'::text, 'webm'::text])
      and source_url ~* '^https://[^/@[:space:]]+'
    )
    or (source_type = 'rtmp'::text and source_url ~* '^rtmps://[^/@[:space:]]+')
    or (source_type = 'srt'::text and source_url ~* '^srt(s)?://[^/@[:space:]]+')
  );
