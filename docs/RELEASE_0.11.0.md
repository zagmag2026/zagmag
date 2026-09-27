# Release 0.11.0 — Phase 11 Reports + History

## Added
- Date-wise Booking Report
- Current Given Items report
- Overdue Returns report
- Item-wise History
- Customer-wise History
- Category-wise Stock
- Server-side date/category/item/customer/status/search filters
- Server-side pagination
- WhatsApp / Call actions where customer context exists
- Open Booking action from booking-related reports
- Report read-performance indexes (`0005_report_indexes.sql`)

## API efficiency
- One report bootstrap request on first page load
- Only the selected report is fetched
- No report auto-polling
- One API request per Apply/Page/Report switch

## Preserved
- No pricing/payment/deposit module
- No laundry/repair/maintenance workflow
- Choli seed and Free Size default
- Authentication, Category, Item, Customer, Booking, Pickup, Return, Dashboard and Public Catalog modules

## Validation
- D1 migrations 0001–0005: PASS
- Foreign key check: PASS
- Report sample-data quantity audit: PASS
- Current Given / Overdue audit: PASS
- Category Stock audit: PASS
- Worker/Admin/Public TypeScript source checks: PASS
