# Zhagmag Dresses 0.14.5 — Phase 14F

## Operational Card Item Images

- Every booked item is rendered individually on Booking, Pickup, Return, Overdue, Dashboard, Customer History and booking-related Report cards.
- Primary image is shown when available.
- Missing image keeps the item visible with a clean placeholder.
- Frontend payload capture is synchronous before card render to prevent the mobile race where the legacy text summary rendered but visual items did not.
- Existing bundled Worker enrichment remains the image/data source; no N+1 or per-item browser requests were added.
- Phase 14C paired-date behavior and Phase 14E Cloudinary secure upload remain unchanged.
- No database migration.
