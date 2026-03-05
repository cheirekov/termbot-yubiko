# TKT-0228 — OpenPGP NFC two-tap + PIV/FIDO2 NFC race fix

## Status: DONE
## Priority: HIGH
## Epic: TKT-0224

## Problem
Three NFC-related failures observed in termbot-report-20260302-165725.txt:

1. **OpenPGP auth** (`SK_ACTIVITY_OPENPGP_AUTH_ERROR`): PIN dialog shown AFTER NFC
   tap → user entered PIN → NFC connection had already expired → auth failed.

2. **PIV import** (`PUBKEY_PIV_IMPORT_FAILED: IllegalStateException`): NFC callback
   spawned `new Thread(...)` before calling `importPivSecurityKeyFromConnection` →
   thread scheduling latency allowed the NFC IsoDep connection to expire.

3. **FIDO2 import** (`PUBKEY_FIDO2_IMPORT_FAILED: ApduException`): same thread-spawn
   race condition as PIV.

## Root Cause
- OpenPGP: wrong UX ordering — PIN collected post-tap instead of pre-tap.
- PIV/FIDO2: `startYubiKitImportDiscovery` wrapped NFC import work in
  `new Thread(() -> importXxx(c)).start()`. The YubiKit callback is already on a
  background thread; the extra thread spawn introduced a window where the NFC
  `IsoDep.transceive()` fired with the connection dead → `IllegalStateException`.

## Fix

### SecurityKeyActivity.java
- Added `private volatile char[] mPendingOpenPgpPin` field.
- `onCreate` OPENPGP case: immediately calls `promptForOpenPgpPin`; on confirm
  stores PIN in `mPendingOpenPgpPin`, shows `security_key_openpgp_tap_now` toast.
- `handleOpenPgpDiscovery`: if `mPendingOpenPgpPin != null`, uses stored PIN and
  creates bridge immediately while NFC connection is live (no dialog on NFC thread).
  Falls back to post-tap PIN dialog for USB (where connection persists).
- `onDestroy`: wipes `mPendingOpenPgpPin` with `Arrays.fill(..., '\0')`.

### PubkeyListActivity.java
- `startYubiKitImportDiscovery`: removed all four `new Thread(() -> importXxx(c)).start()`
  wrappers for both NFC and USB callbacks. Import methods called directly on the
  callback thread, eliminating the NFC connection expiry race.

### strings.xml
- Added `security_key_openpgp_tap_now`: "PIN accepted. Now hold your YubiKey to the back of the phone."

## Files Changed
- `repos/termbot-termbot/app/src/main/java/org/connectbot/SecurityKeyActivity.java`
- `repos/termbot-termbot/app/src/main/java/org/connectbot/PubkeyListActivity.java`
- `repos/termbot-termbot/app/src/main/res/values/strings.xml`

## Build
✅ assembleDebug GREEN — android_build_2026-03-02T21-40-14+02-00.log
