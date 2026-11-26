# RAMİ STORAGE source archive

This directory preserves reusable RAMİ STORAGE source from the private `Ramistar900/rami-storage` repository.

## Scope
- Archived Edge Function source files are copied from `feature/hls-drive-packaging` at the time of archival.
- Related historical SQL migrations are retained for reference only.
- This is an archive, not an active deployment. Do not deploy these functions or apply these migrations automatically.
- The `rami_storage` database schema was intentionally removed from the shared Supabase project. These migrations do not restore its data.

## Archived Edge Functions
1. `rami-google-drive-import`
2. `rami-google-drive-read`
3. `rami-hls-packager`
4. `rami-storage-admin-check`
5. `rami-storage-channel-admin`
6. `rami-storage-media-library`
7. `rami-storage-owner-bootstrap`

## Not archived: source unavailable
The deployed Edge Function `rami-google-oauth` was active in Supabase, but no corresponding source file was present in the inspected repository default branch or the inspected HLS packaging branch, and repository code search returned no match. Its source must be recovered from a trusted original before that deployed function can safely be deleted. No placeholder or guessed implementation is included.

## Secrets and safety
- Runtime secret names such as `SUPABASE_SERVICE_ROLE_KEY` are environment-variable references, not secret values.
- Never commit actual access tokens, OAuth client secrets, service-role keys, private keys, or production credentials.
- Keep the original Google Drive media files untouched.
