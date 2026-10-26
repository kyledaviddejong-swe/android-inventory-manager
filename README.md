# Android Warehouse Inventory

A Java Android app for keeping track of stock: create an account, find an item, change its quantity or location, and get back to work. Inventory and account settings persist locally in SQLite.

**Included:** the Android Studio project, Gradle wrapper, and focused JUnit/Robolectric tests. Android 14 (API 34) or newer is required to run the app.

## What it does

- Creates local accounts and checks credentials at login. Passwords use salted PBKDF2-HMAC-SHA256 hashes.
- Adds, edits and deletes inventory records, with required-field checks and nonnegative quantities. Deletion requires confirmation.
- Displays stock in a RecyclerView, with search by item name or warehouse location and a zero-stock filter.
- Offers optional SMS alerts when an item is added at zero stock or changes from a positive quantity to zero. Editing an already-empty item does not resend the alert.
- Keeps SMS settings per account. Alerts can be enabled or disabled, and inventory remains usable when SMS permission is denied or the device has no messaging support.

All accounts on an installation use the **same warehouse inventory**. Account credentials and SMS preferences are separate. There is no remote server or cloud synchronization.

## Run it

1. Open this repository's root folder in an Android Studio version that supports **Android Gradle Plugin 9.3.3**.
2. Install **Android SDK Platform 37.0** and **Build Tools 36.0.0** using the SDK Manager. Accept the SDK licenses.
3. Let Gradle sync finish. The included wrapper uses **Gradle 9.5.0**, and the supplied daemon configuration selects **JDK 25**. A compatible JDK can be provisioned automatically on the first run, which requires internet access.
4. Start an Android 14 or newer emulator, select the `app` configuration, and run it.

Create a disposable account, log in, and add a few sample items. For example, add **Bolts / 12 / A1** and **Washers / 0 / B2**, then try search, the out-of-stock filter, editing, and deletion.

To build from a terminal after configuring the Android SDK:

```bash
./gradlew :app:assembleDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`. The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk` and is intentionally excluded from source control.

SMS is optional. Leave it disabled for the ordinary demo. Real sending requires a suitable device/SIM, permission, and a destination you control or have permission to contact. Carrier charges may apply. Submitting a message to Android does not confirm delivery.

## Tests

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Verified October 26, 2026: the debug build succeeded, **all 15 tests passed**, and lint reported no errors.

The tests cover password verification, account persistence, inventory CRUD and search, invalid quantities, database migration, account-specific SMS preferences, disabling alerts, and permission-screen recreation. They run with Robolectric on the JVM and do not send real SMS messages.

See [verification and remaining limits](docs/VERIFICATION.md) for what was actually checked.

## Code map

| File | Responsibility |
|---|---|
| `MainActivity.java` | Account creation and login, with password work off the UI thread |
| `InventoryDatabaseHelper.java` | SQLite schema, migration, account queries and inventory operations |
| `InventoryActivity.java` / `InventoryAdapter.java` | Inventory screen, dialogs, filtering and row actions |
| `SmsPermissionActivity.java` / `SmsNotificationHelper.java` | Saved alert preferences, Android permission and message submission |
| `PasswordHasher.java` | Salted password hashing and verification |

The Java source is under [app/src/main/java/com/example/kyle_dejong_inventory_app](app/src/main/java/com/example/kyle_dejong_inventory_app). XML layouts are under [app/src/main/res/layout](app/src/main/res/layout).

## Background

I built this as an undergraduate mobile-development project, working through the Android activity lifecycle, adapters, SQLite persistence and runtime permissions. The inventory workflow is the main focus: keeping common actions on one screen while separating the interface from database operations.

This portfolio edition includes a small set of review fixes. [Development notes](docs/DEVELOPMENT.md) preserve my original reflection, and [credits](CREDITS.md) distinguish the submitted work from the later cleanup.
