# Verification and scope

Reviewed October 26, 2026.

## Recorded results

The portfolio source was built and tested on Linux with the project's Gradle 9.5.0 wrapper, Android Gradle Plugin 9.3.3, Eclipse Temurin JDK 25.0.3, Android SDK Platform 37.0 and Build Tools 36.0.0.

| Check | Result |
|---|---|
| `:app:assembleDebug` | Passed. Debug APK generated from the cleaned source. |
| `:app:testDebugUnitTest` | 15 tests passed, zero failures, errors or skipped tests. |
| `:app:lintDebug` | Passed with zero errors and 10 non-blocking warnings. |
| XML resources | Parsed successfully. |
| Publication files | No personal email, signing keys, runtime databases, school handouts, IDE workspace files or compiled APK included. |

The lint warnings concern possible background overdraw, unused or redundant resources/namespaces, button styling, and dependencies declared outside the version catalog. They do not prevent the build. The build also reports Gradle deprecations and a native-access warning from a test dependency. The documented Gradle/JDK versions passed; a future major build-tool upgrade has not been tested.

## What the tests establish

- Correct passwords verify; incorrect, empty, malformed and plaintext values do not. Equal passwords receive different random salts, and Unicode and intentional whitespace are preserved.
- Accounts and stock persist when the database helper is reopened. Duplicate usernames and negative inventory quantities are rejected. Search matches item names and locations, and selection arguments prevent the tested SQL-injection input from changing query meaning.
- Inventory updates and deletion use internal row IDs, including when visible item names repeat.
- Upgrading the submitted version-2 database preserves existing account IDs, valid logins, inventory and SMS preferences while replacing plaintext password values with hashes.
- Account creation and login work through the activity's background-worker/UI path, including a rejected password and successful navigation to inventory.
- SMS preferences stay account-specific. Disabling alerts preserves the saved destination, pending phone input survives activity recreation, and denied SMS permission does not block inventory operations.

Robolectric tests used Android API 34. They exercise Android components and SQLite on the JVM. They are not a physical-device or emulator run, and no real SMS messages were sent.

## Focused changes from the submission

- Added salted PBKDF2-HMAC-SHA256 password storage with 600,000 iterations, a 16-byte random salt and a 256-bit derived key. Account operations run off the UI thread. The database migrates existing records instead of deleting accounts or inventory.
- Excluded the database from Android backup/transfer rules and disabled application backup.
- Added an in-app SMS disable control, preserved pending permission state, handled interrupted permission prompts, checked settings-save results, and refreshed status after returning to the settings screen.
- Made SMS settings scrollable and used a light theme consistently with the application's existing light palette. This prevents night-mode defaults from supplying light input text on the app's light field backgrounds.
- Split longer SMS content into multipart messages when needed. A successful API call is described as submission, not delivery confirmation.
- Replaced the generated example tests with focused regression tests, added build/run documentation and ignore rules, and omitted IDE files and the old debug APK. Gradle's initial-download timeout was increased to 60 seconds.

## Remaining limits

This is a local warehouse demonstration. Accounts share one installation's inventory, and there is no backend, role-based authorization, server authentication, synchronization or password recovery. Password hashing does not encrypt the complete database or turn a local login into a security boundary on a compromised device. Use disposable demonstration data.

Ordinary inventory queries still run on the UI thread and reload the list. That is reasonable for this project's small dataset; large inventories would call for background queries, pagination and incremental list updates.

The original author reports Android Emulator testing in the [development notes](DEVELOPMENT.md). This review did not repeat a device/emulator visual walkthrough. No screenshots are presented as newly verified runtime evidence. Carrier delivery, SIM selection, device-specific SMS behavior, accessibility at large font sizes, and broader device/layout coverage remain outside the verified scope. SMS delivery has no acknowledgment or retry queue.

## References for review changes

- [Android Gradle Plugin 9.3 compatibility](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
- [Robolectric setup](https://robolectric.org/getting-started/)
- [OWASP password-storage guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
