# Credits and project history

I built the original Java Android app as an undergraduate project. It included account creation and login, SQLite storage, a RecyclerView inventory screen, adding/editing/deleting stock, search, zero-stock filtering, and optional SMS alerts.

I used AI assistance with unfamiliar Android APIs and boilerplate, as described in the [development notes](docs/DEVELOPMENT.md). The original [AI acknowledgment](AI_Acknowledgement.txt) is included unchanged.

The AI-assisted review after submission added password hashing and database migration, moved account work off the UI thread, disabled database backups, improved SMS settings and permission handling, fixed theme consistency, and added multipart SMS support, regression tests, and documentation. [Verification notes](docs/VERIFICATION.md) list those changes and the test results. The school submission remains unchanged.

Android/AndroidX, Material Components, Gradle, JUnit and Robolectric are third-party dependencies with their respective licenses. Generated Gradle wrapper scripts retain their copyright/license notices. The default Android Studio launcher artwork is retained. No school assignment handouts, rubrics or instructor materials are included.
