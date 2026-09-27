# 0.14.4 — Phase 14E — Cloudinary Secure Item Images

## Added
- Secure signed Cloudinary upload in Admin Item Master.
- Direct browser → Cloudinary image transfer; large image bytes do not pass through the Worker.
- Up to 8 item photos; zero photos is supported and continues to show the placeholder.
- First image is Primary, with Make Primary and Remove actions.
- D1 mapping of Cloudinary public IDs for best-effort cleanup of removed saved images/items.
- `keep_vars = true` on Wrangler configs to retain dashboard-added Cloudinary variables across deploys.

## Required Worker configuration
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET` (Secret)

## Database
- Apply `database/migrations/0009_cloudinary_item_assets.sql`.

## Build/deploy impact
- Admin Web: changed
- Worker/API: changed
- D1: migration 0009
- Public Web feature source: unchanged (version identity only)
