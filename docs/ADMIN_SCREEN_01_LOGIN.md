# Admin Android — Screen 1: App Entry / Login

Status: **Approved / screen contract locked**

This file defines the final Admin Android Screen 1 behavior. It is subordinate to `UI_GUIDELINES.md`, but its Screen 1-specific decisions are authoritative unless the user explicitly changes them later.

## Final hierarchy

1. Dynamic Logo
2. Dynamic Shop Name
3. `Sign in`
4. `Mobile number` — live validation
5. `Password` — visibility action
6. `Sign in` — icon + name button

No subtitle, helper paragraph, tutorial card, language selector, email-login field, register action or unrelated content is shown.

## Dynamic branding

- Shop name must be dynamic, not hard-coded in the active entry screen.
- Admin Android uses the English dynamic shop name.
- Logo must be dynamic through the public/auth branding contract.
- Load cached branding immediately where available.
- A valid authenticated session may refresh branding from the auth response without an extra branding request.
- When signed out, public bootstrap may refresh branding.
- Do not repeatedly fetch branding on recomposition or normal navigation.
- If branding refresh fails, retain last valid cached branding.
- First-use fallback shop name is `Your Shop Name`.
- If no valid dynamic logo can be loaded, use the approved local app-logo fallback.
- Remote logo URLs must use HTTPS.

## Mobile field

- Login identifier is **mobile number only**.
- Do not show `Email or mobile`.
- Field label: `Mobile number`.
- Inside-field hint may read `10 digit mobile number`; it is guidance, not a prefilled value.
- Numeric/phone keyboard only.
- Accept digits only.
- Maximum length: 10 digits.
- Validation is live while typing.
- A non-empty value with fewer than 10 digits shows concise inline validation.
- A 10-digit value is valid for Screen 1 input validation.
- No separate helper card or paragraph for mobile format.

## Keyboard behavior

- Validation must not automatically hide the keyboard.
- Login failure must not automatically hide the keyboard.
- Do not clear focus merely because a validation/error state changed.
- Successful navigation away from Login may naturally remove the keyboard because the screen is replaced.
- The screen must remain IME-safe so the focused field and primary action stay usable.

## Password field

- Use the shared form-field pattern.
- Use Google Material Rounded `Visibility` / `Visibility Off` icons.
- The visibility action is icon-only and must provide both tooltip text and accessibility content description.
- Password requires a minimum of **8 characters** for Screen 1 input validity.
- Password keyboard action is `Done`.
- `Done` submits only when the mobile is exactly 10 digits and the password has at least 8 characters.
- Raw password must never be written to preferences, files, logs, database rows or other persistent client storage.
- Password input must use non-saveable screen memory, not `rememberSaveable` or another state-restoration mechanism.
- On successful sign-in, the Login screen is disposed and the raw password value is discarded.
- On explicit logout or a fresh Login screen, the password starts empty.
- Password visibility starts hidden on every fresh Login screen and must not be restored after process recreation.
- Recoverable login failure may keep the currently typed password in the active screen so the user can correct the mobile/password without retyping unnecessarily; that temporary value must not survive a process/app restart.

## Sign in action

- Button uses **Google Material Rounded icon + `Sign in` name**.
- Full-width compact primary action.
- Disabled until mobile is exactly 10 digits, password has at least 8 characters, environment is usable and no login request is already active.
- Busy state prevents duplicate submit.
- Double tap must not create duplicate login requests.
- Login API continues to use the existing server identifier contract, but Android supplies the validated mobile number only.

## Error behavior

- Errors are concise and user-facing.
- Do not expose Worker, D1, endpoint names, build variables or internal configuration details.
- Invalid credentials use a concise mobile/password error.
- Timeout and rate-limit messages remain concise.
- Existing entered mobile/password values remain on recoverable login failure.

## App entry / session check

- On app start, show compact dynamic/cached branding with a small loading indicator while session state is checked.
- Do not show an explanatory `Checking secure session` paragraph.
- If the session is valid, proceed directly to the authenticated app without showing Login.
- If the session is not valid, show the Login screen.
- App close or force-stop does **not** itself mean logout; a still-valid secure session may restore authenticated entry.
- Session expiry or an authenticated API `401` clears the local secure session and returns the app to Sign in.

## Credential/session policy

- Android saves the authenticated **session only**, never the raw password.
- The session cookie remains protected by the existing Android Keystore + AES-GCM local session store.
- A valid persisted session allows direct authenticated entry after app reopen.
- Mobile number retention is optional; password retention/prefill is prohibited.

## Logout policy

- Logout is explicit.
- While the current session cookie is still available, the app first attempts the server logout endpoint so the server can invalidate the session.
- Local secure session clear is mandatory even if server/network logout fails.
- After local session clear, authenticated cached screen state is cleared and root auth state becomes Signed out.
- After logout, Android Back must not expose the previously authenticated UI; the root is the Login state rather than an authenticated navigation-stack destination.
- A fresh Login screen after logout has an empty password and hidden password visibility state.

## Responsive/accessibility

- Phone-first, tablet-compatible using the same adaptive screen structure.
- Respect status/navigation bars.
- Use a compact centered form with a reasonable maximum content width on larger screens.
- Do not clip text at increased system font sizes.
- Preserve practical touch targets.

## API-call discipline

- Cached branding is shown immediately.
- Successful `/api/auth/me` may supply branding for an existing session.
- Public bootstrap is used as a signed-out branding fallback rather than polling.
- No periodic branding refresh.
- No request on recomposition.
- No Cloudflare/API request caused by mobile live validation.

## Completion gate

Screen 1 is complete only when:
- dynamic/cached logo and shop-name behavior is wired,
- mobile-only 10-digit live validation works,
- password visibility icon + tooltip works,
- keyboard is not programmatically hidden by validation/failure,
- raw password is non-saveable and never persisted,
- valid secure session restores direct authenticated entry,
- logout attempts server invalidation and always clears the local secure session,
- logout cannot return to authenticated UI via Back,
- Sign in is icon + name with duplicate-submit protection,
- static Android audit passes,
- staging APK compiles successfully.
