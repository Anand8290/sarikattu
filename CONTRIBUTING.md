# Contributing

Thanks for helping. SplitLite stays deliberately small, so:

- **Bugs and fixes**: pull requests are welcome.
- **New features**: open an issue first so we can agree it belongs in a minimal app.

## Before you open a pull request

1. Set up a Supabase project and `local.properties` as described in the README.
2. Run `./gradlew testDebugUnitTest` from `android/`.
3. Changes to money maths (`domain/`) need unit tests.
4. Database changes go in a **new** migration file (`supabase/migrations/0004_….sql`). Never edit one
   that has already been released.
5. Keep the app free of Google Play Services and other proprietary libraries, so it can be listed on F-Droid.

## Code style

The Kotlin official code style (Android Studio's default). Keep screens simple: state lives in the repository
`StateFlow`s, and `AppViewModel.run` handles loading and errors.
