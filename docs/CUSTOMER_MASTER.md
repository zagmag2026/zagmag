# Customer Master

Status: **Admin Android Screen 3 implemented / finalized / locked**

Customer Master is the single customer identity source used by bookings and operational screens.

## Fields
- Name
- Mobile Number — the only user-facing contact number
- Address
- Notes where supported by the relevant backend flow

Alternative Mobile is not part of the current product UI or current customer workflow. Existing legacy database values, if any, are ignored by the Admin Android UI and must not be shown, selected for Call/WhatsApp, or reintroduced through a separate chooser.

## Contact actions
- Customer has one Mobile Number only.
- Tapping **Call** always opens the same shared confirmation bottom sheet before launching the dialer; no direct-dial path is allowed from the initial button.
- Call confirmation uses **Person/User icon → Customer Name**, **Call icon → Mobile Number**, then `Cancel | Call`.
- Tapping **WhatsApp** loads the applicable template group first, then reaches the shared message-preview confirmation bottom sheet.
- WhatsApp confirmation shows the customer name, Mobile Number, full generated message preview, and `Cancel | WhatsApp` actions.
- The confirmation sheet uses the same approved bottom-sheet family as New/Edit Customer: drag handle, clear title, close `X`, rounded top corners, compact spacing, and safe bottom actions.
- Invalid/blank Mobile Number must not launch an external app; show the shared transient error message instead.
- Prevent duplicate external launches from double taps.
- No number chooser is shown.

## Customer card
- Show customer name, Mobile Number and Address using the global icon + value information pattern.
- Directly below Address, show `Total Booking` and `Active Booking` in one fixed horizontal row at **50% / 50% width** with centered text.
- Active customer actions stay in one compact horizontal row in this fixed order: **Edit | Archive | Booking | Call | WhatsApp**. Permission-hidden actions are omitted without reordering the remaining actions.
- `Booking` from a customer card opens New Booking with that customer already selected in Step 1; the operator must not need to search/select the same customer again.

## Confirmation wording / safety
- Active-customer reversible removal is labelled **Archive** everywhere: card action, dialog title and confirm action.
- Archive confirmation explains that the customer moves to Archived while booking history is preserved and can be restored.
- Restore remains confirmation-first through the shared neutral confirmation dialog.
- Permanent Delete uses the shared destructive confirmation dialog and requires the exact phrase **DELETE CUSTOMER** before `Delete permanently` is enabled.
- Permanent-delete copy states that the customer plus preserved booking/lifecycle history will be removed permanently and cannot be undone.
## Archive / restore / permanent delete
- Archive marks only the customer inactive/archived. Existing booking history remains stored; normal Booking list views may hide it while the customer is archived and it becomes visible again after Restore.
- Restore is confirmation-first. The operator must confirm **Restore customer?** before the restore API call runs.
- A successful Restore uses the normal transient success feedback (`Customer restored successfully.`).
- Permanent Delete remains available only from Archived and only when no active booking exists.
- After Permanent Delete from Archived, stay on **Archived** when the authoritative global archived total is still greater than zero, even if the current search has no visible rows. Switch to **Active** only when the authoritative archived total becomes zero.
- Permanent Delete removes the archived customer and its completed/cancelled booking lifecycle data in one dependency-safe D1 batch, including pickup/return and booking-close event children before booking items/bookings/customer.
- The destructive batch includes its audit insert so a dependency/audit failure cannot be reported as success after only part of the intended permanent-delete sequence.
- Archive/Restore audit metadata describes booking history as preserved; it must not claim bookings were archived/restored when only customer visibility changed.
## Customer form / keyboard
- New Customer and Edit Customer reuse the same bottom-sheet form.
- The focused field and required `Cancel | Save` action row must remain visible with the keyboard open.
- The field body is scrollable; the bottom action row remains fixed above IME/system navigation insets.
- Keyboard action order is `Name → Next → Mobile Number → Next → Address → Done`.
- `Done` preserves the Address value, clears focus and reliably hides the keyboard without closing the sheet.
- New/Edit Customer reuses the shared reliable dismiss helper (Compose focus clear + keyboard hide + platform IME fallback/post-focus retry); no screen-local one-off dismiss logic.
- Success/error feedback follows the global transient popup message rule.
- Customer lists support the approved +10 incremental loading and swipe-down refresh pattern.

## Customer form validation
- **Name** and **Mobile Number** are required and show `*` in the shared Customer form.
- Save remains disabled until Name is nonblank and Mobile Number contains exactly 10 digits.
- After interaction, blank Name shows `Name is required.`; blank Mobile shows `Mobile number is required.`; partial Mobile shows the 10-digit validation message.
- Address remains optional. ViewModel/API validation remains authoritative after the same UI checks.
## WhatsApp template linkage
- Customer-card/general customer WhatsApp has no Booking context, so it uses the central **Other** general-message group.
- Other contains general messages only: Shop Address, Working Hours, General Information, Thank You, Holiday / Shop Closed, Contact Us and Custom General Message.
- Multiple applicable active templates open the shared template-selection sheet; exactly one skips selection and opens Preview directly.
- Customers keeps the shared visible action-error popup; the same no-silent-failure feedback contract is reused by Dashboard, Booking List and Booking Details.
- No customer-screen hard-coded WhatsApp message is allowed.

## Loading / empty / retry behavior
- Initial customer-list failure shows the shared Error + Retry state and must not also show `No customers found.` / `No archived customers.`.
- A successful empty response is the only source of the customer empty state.
- Pull-to-refresh keeps previously loaded customer rows visible on failure and shows the passive load error inline.
- Load-more failure preserves existing rows and stops automatic append retries until the operator explicitly refreshes or changes criteria.
