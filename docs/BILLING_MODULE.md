# Zhagmag Dresses — Billing Module V1

Status: **Current main implementation; staging validation pending for the booking-linked lifecycle batch; production untouched**

## Scope
Billing V1 is **Order-linked**. New standalone/direct Bill creation is not available.

Every new Bill must be linked to an existing Order/Booking. One Order can have at most one Bill.

Historical standalone Bills, if any exist in non-production/history data, remain view-only and are not eligible for new edits/finalization/payment/cancellation/deletion mutations.

### Item Master
- Item Master has one fixed numeric `Rent` field.
- Rent is the default rental rate only.
- A Quotation auto-fills Rent Rate from Item Master.
- Quotation Rent Rate remains editable so the actually charged rent may differ from the Item Master default.
- Final Bills store snapshots; later Item Master Rent changes never alter old Final Bills.
- Deposit, GST, late fee, damage charge and miscellaneous pricing remain out of scope.

### Booking Advance
- New/Edit Booking **Payment** step shows `Advance Amount`; the preceding Preview step is review-only.
- Initial value is `0`.
- If the value is cleared, the field placeholder is `Advance Amount`; there is no extra helper label below the field.
- Focusing an amount/quantity field selects the complete current numeric value so typing replaces it directly.
- Rent is not shown in the Booking flow.
- Advance is saved for Reserve, Confirm and Directly Pickup.
- A linked Quotation auto-fills the saved Booking Advance.

## Bill entry
There are two entry paths, both linked to an Order:

1. **Booking / Order action → Bill**
   - Existing Bill → open that Bill.
   - No Bill → open a Booking-seeded Quotation editor.
   - Cancelled Order with an existing Bill → open the historical Bill.
   - Cancelled Order with Advance settlement but no Bill → open the read-only Advance Settlement view.
   - Cancelled Order with no Bill and no Advance → Bill action is hidden.

2. **Bills → Add Bill → Select Order**
   - Opens **Select Order**.
   - Shows only Orders that do not already have a Bill.
   - Search supports Order/Booking number, Customer name and Mobile.
   - Selecting **Create Quotation** creates the linked Draft server-side, then opens it.
   - Cancelled Orders are not eligible for a new Bill.

Direct/standalone Customer + Item Bill creation is removed.

## Full Return Billing handoff
When a Return mutation makes the Order **Full Returned**:
- if no Bill exists, Worker creates one linked Draft from that Order;
- if a Draft already exists, reuse it;
- if a Final/Cancelled Bill already exists, reuse that historical Bill and never create a duplicate;
- Android opens the related Billing page after the successful Return;
- Return success is not rolled back if Billing Draft preparation/opening fails; a visible Billing error is returned while the Return stays saved.

One Order = maximum one Bill remains authoritative at D1/Worker level.

## Master deletion and financial history
- Customer permanent deletion never deletes Bills or Bill Items.
- Bill customer/mobile/address/booking/item snapshots remain finance-authoritative after Customer or Item master deletion.
- Customer permanent deletion detaches Bill → Booking live references before completed/cancelled operational Booking history is removed, then hides/scrubs the Customer master via a tombstone kept only for D1 FK integrity.
- Item permanent deletion detaches `bill_items.item_id` while preserving Item Code/Name/Category/Qty/Rent/Amount snapshots. Items with Booking history remain non-deletable and must be archived.
- Historical detached Bills remain viewable/exportable for finance; mutation/contact actions that require a live Order/Customer remain unavailable.

## Bill items
A linked Draft starts from the Order Items and stores:
- Item ID reference
- Item Code snapshot
- Item Name snapshot
- Category snapshot
- Qty
- editable Rent Rate
- Amount = Qty × Rent Rate

Draft quantity/rate editing never changes the underlying Booking quantities.

## Amount calculation
- Item Total = sum of line Amount.
- Discount is a ₹ amount only; default 0.
- Bill Amount = Item Total − Discount.
- Advance Received defaults from the linked Booking and remains editable while the Bill is Draft.
- Other Received is the additional amount received beyond Advance.
- Total Received = Advance Received + Other Received.
- Balance Due = Bill Amount − Total Received.
- Payment collection is Cash only; no Payment Mode selector is shown.
- Payment Status is derived automatically: Total Received = 0 → Pending; 0 < Total Received < Bill Amount → Part Received; Total Received >= Bill Amount → Full Received.
- Discount cannot exceed Item Total.
- Advance Received cannot exceed Bill Amount.
- Advance Received + Other Received cannot exceed Bill Amount.

Worker/D1 recalculates and validates monetary totals; Android values are not authoritative.

## Lifecycle
`Draft → Final → Cancelled`

Quotation:
- always linked to a **confirmed** Order for newly created Bills; Reserved Orders do not have Bills;
- may be created from Booked onward and saved before Return completion;
- Qty, Rent Rate, Discount, Advance Received, Other Received and Notes remain editable;
- Payment Status is derived from the saved amounts and is not manually selected;
- may be deleted while the linked Order is still active.

Final:
- can be created only after authoritative 100% Return completion: at least one quantity was picked up, Pickup Pending = 0 and Return Pending = 0;
- receives a unique financial-year Bill No, e.g. `ZD/26-27/000001`;
- item/customer/date/notes/amount snapshots are locked and historical values cannot be changed afterward;
- cannot be deleted.

Cancelled:
- remains historical;
- manual Final-Bill cancellation requires a non-empty reason;
- if the linked Order itself is cancelled, a linked Draft or Final Bill is automatically moved to Cancelled with audit history;
- cancellation user/time/reason are preserved.

Finalization and Bill number allocation are server-side and uniqueness-protected. The Worker rejects Finalize before full Return with `BOOKING_RETURN_INCOMPLETE`; Android also keeps Finalize disabled until the same quantity-derived condition is true. Duplicate submit/request keys must not create duplicate Bills.

## Booking cancellation Advance settlement
If an Order is cancelled before pickup and `Advance Amount > 0`, cancellation requires exactly one settlement:

- **Full Refund** → Refund = full Advance; Retained = 0.
- **Partial Refund** → Refund must be greater than 0 and less than Advance; Retained = Advance - Refund.
- **No Refund** → Refund = 0; Retained = full Advance.

The original Advance is never overwritten to zero. D1 stores:
- original Advance Amount
- Advance Refund Amount
- Advance Settlement Status
- settled timestamp/user

If no linked Bill exists, Billing can show the cancelled Order's read-only **Advance Settlement** page. If a linked Draft/Final Bill exists, Order cancellation also closes that Bill as Cancelled.

## Android surfaces
Billing workspace has these views:
1. Bills List
2. Select Order
3. Create / Edit linked Bill
4. Bill Details
5. read-only Advance Settlement for cancelled Orders without a Bill

More → Bills opens Billing.

Booking Details tabs are **Details | Pickup | Return | Bill | History**. The fourth-position Bill tab hosts the linked Generate/Edit/Detail Billing workspace inline. Booking Details/Summary Bill actions open the related linked target directly; the Bills List must not flash before the target.

Bill Generate/Edit uses this final order:
**Bill Header → Booking & Customer → Items (x) → Pickup / Return Status → Amount Summary → Notes → Actions**.

Bill Details uses:
**Bill Header → Booking & Customer → Items (x) → Amount Summary → Notes → Actions**.
Pickup / Return Status is intentionally removed from the on-screen Bill Details view.

- Bill Header shows Bill No., a **compact calendar + DD-MM-YYYY Bill Date control at the top-right**, and a Draft / Finalized status badge.
- Booking & Customer shows Customer Name → Mobile → optional Address → Order/Booking ID with consistent icons.
- Bill list cards reuse the same identity order and final terminology: formatted Bill Date, **Bill Amount** and **Balance Due**; legacy `Net` / raw ISO-date wording is not shown.
- Select Order cards use Customer Name → Mobile → optional Address → Order ID and do **not** repeat Pickup/Return badges. Bottom actions are **Order Preview | Create Quotation** in one line.
- Bills List linked cards use **Order Preview | Edit** for Quotation and **Order Preview | View** for Finalized; standalone/legacy Bills hide Order Preview.
- Order Preview is one shared read-only Order Card component reused by Select Order and Bills List.
- **Items (x)** contains all items in one section/card; each item is a separate compact row with image, name/code/category, Qty × Rent Rate and Line Amount. Quotation Rent Rate is seeded from Item Master and remains editable; Finalized values are stored as snapshots.
- Draft editor Pickup / Return shows Done / Pending, Date and Day. Bill Details omits this block.
- Finalized Bill PDF retains Pickup / Return and shows **Status + Date + Day + actual event Time** using pickup/return event timestamps. If an actual event time is unavailable, no fake time is rendered.
- Amount Summary is exactly: Item Total − Discount = Bill Amount; Advance Received + Other Received = Total Received; Bill Amount − Total Received = Balance Due. The top-right of Amount Summary uses the shared reusable **StatusBadge** family for **Bill Amount ₹X | Balance Due ₹Y**; directly below it, the same reusable badges show **Bill Status | Pending Payment / Part Payment / Full Payment**. The duplicate Bill Amount and Balance Due value rows are not rendered below the badges.
- Notes are editable only in Draft.
- Quotation Actions reuse the global shared **SoftActionButton** family with icon + label: **Save Quotation | Finalize Bill**. Finalize is disabled when **Balance Due > ₹0** and remains disabled/server-rejected until **100% Return + Balance Due = ₹0**.
- Finalized Actions are unboxed shared buttons in one horizontal row: **View | Share | Download | Print**. Existing View/Share/Download/Print behavior is unchanged.

Booking lifecycle action visibility is state-based: Edit is Reserved-only; Reserved has no Bill. From Booked onward, Bill is available and Edit is removed.

Final Bill PDF follows the same hierarchy and amount terminology as the screen. It includes shop branding, Bill No./Date/status, Booking & Customer, Items (x), Pickup/Return, Item Total, Discount, Bill Amount, Advance Received, Other Received, Total Received, prominent Balance Due, Notes, footer/business details, Thank You and Authorized Signatory treatment where available.

## WhatsApp Centre
More → WhatsApp Centre uses the central template infrastructure.

Billing linked actions:
- BILL_DETAILS
- PAYMENT_PENDING
- FULL_AMOUNT_RECEIVED
- BILL_CANCELLED

Billing placeholders:
- bill_no
- bill_date
- total_rent
- discount_amount
- net_amount
- advance_amount
- balance_amount
- payment_status

Bill WhatsApp uses the shared failure-safe launcher. WhatsApp Activity records only system preparation Prepared/Failed; it never claims delivery/read status.

## Reports
Screen 8 sections:
`Overview | Operations | Inventory | Customers | Billing | Activity`

Billing reports:
- Billing Overview
- Bills Report
- Pending Balance
- Full Amount Received

**Billing reports and all monetary report data are OWNER-only.**
STAFF must not see the Billing Reports section and Worker authorization rejects direct Billing monetary report/API access from STAFF even when Reports permission exists.

Billing reports reuse the existing paging/PDF architecture:
- 10 records initial/next page
- no polling
- no per-row calls
- one complete explicit export request
- unchanged export caching rules

## Permissions
Operational Billing:
- OWNER: allowed
- STAFF: requires BOOKINGS permission

Billing Reports:
- OWNER only

Bills List:
- loads 10 initially and appends next 10 on bottom scroll;
- search/filter changes restart at page 1;
- no Previous/Page/Next controls;
- no polling.

## Audit
Bill events use the existing audit log:
- CREATE
- UPDATE
- FINALIZE
- PAYMENT_UPDATE
- CANCEL
- DELETE Draft

Booking cancellation with Advance settlement is also audited on the Booking record. Automatic linked-Bill cancellation creates a BILL cancellation audit.

## Data
Billing base migration:
- `0023_billing_v1.sql`

Booking/Billing lifecycle migration:
- `0024_booking_billing_lifecycle.sql`

Financial-history / partial-payment migration:
- `0025_financial_history_payment_hardening.sql`

Billing Notes migration:
- `0026_billing_notes.sql`

Billing tables:
- bill_sequences
- bills
- bill_items

Existing tables extended include:
- items.rent_amount
- bookings.advance_amount
- bookings.advance_refund_amount
- bookings.advance_settlement_status
- bookings.advance_settled_at
- bookings.advance_settled_by_user_id
- whatsapp_activity_logs billing context

No second backend/database is introduced.

## API / efficiency
Billing uses bounded list/detail/bootstrap/mutation requests only. Select Order loads 10 records per page and appends the next 10. No polling is introduced.

## Release safety
Production remains untouched and requires explicit production authorization after staging validation.
Version remains `0.14.5`, Android versionCode `1`.


## Booking History payment integration
- Booking Details → History merges lifecycle events with linked Bill audit events chronologically.
- Payment history shows Quotation Created/Updated/Finalized/Cancelled and, when values changed, Bill Amount, Advance Received, Other Received, Total Received, Balance Due and Payment Status as old → new values.
- User/Staff and Date/Day/Time reuse the existing Booking History presentation when available.


## Bills List refresh
- The main Bills list supports pull-to-refresh, preserving Search, Payment Status and Bill Status filters and reloading page 1.


## Step 3–7 Billing interaction hardening
- Bill Edit captures a baseline for Bill Date, amounts, notes and Item quantity/rent values. Back/gesture navigation closes directly when unchanged and requires **Discard unsaved changes?** when changed.
- Bill Item rows in Edit and Detail pass the full `imageUrls` list into the shared full-screen gallery; Worker Billing bootstrap/detail returns ordered Item image URL metadata.
- Billing numeric IME navigation follows Qty → Rent across every Item, then Discount → Advance Received → Other Received. Save Draft and Finalize Bill dismiss the keyboard before mutation.
- Bills List distinguishes initial API failure from a successful empty list and provides Retry. Next-page failure preserves loaded Bills and exposes **Retry loading more**.
- Select Order uses the same Loading → Failure/Retry → Successful Empty distinction and next-page retry.
- Bill Detail load failure provides explicit Retry and Back.
- Embedded Booking Details → Bill never remains on an indefinite opening spinner after failure/business block; it renders the reason with Retry + Back.


## Quotation presentation
- The editable internal `DRAFT` Bill lifecycle state is presented in Admin UI as **Quotation**; the underlying D1/Worker status remains `DRAFT` for compatibility and server authority.
- A Quotation can be edited/saved before Finalize and its PDF can be viewed, downloaded, printed and shared directly to the customer's WhatsApp.
- Quotation and Final Bill PDFs use the same layout/content hierarchy but have distinct visual colour themes and document titles.
- Quotation PDF generation does not require a finalized Bill No.; it uses the linked Order/Booking as the reference.
