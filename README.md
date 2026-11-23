# SplitLite

A minimal, open-source app for splitting group expenses. Free, no ads, no accounts, no limits.

- Create a group, share the invite link, and everyone adds expenses from their own phone
- Split equally between any members of the group
- See who owes whom, with the fewest payments needed to settle up
- Record payments (cash, UPI, anything) to clear balances

That's the whole app, on purpose.

## How it works

| Part | Tech |
|---|---|
| Android app | Kotlin, Jetpack Compose, Material 3 (`android/`) |
| Server | [Supabase](https://supabase.com) — Postgres + anonymous auth (`supabase/`) |

There are no sign-ups. Each phone gets an anonymous Supabase account. Joining a group with an invite code
gives that account access to the group. Postgres row-level security means you can only read groups you've
joined, and every write goes through the SQL functions in `supabase/migrations/0003_functions.sql`,
which check membership.

Money is stored as whole numbers of the smallest unit (paise, cents). Balances are never stored. They're
computed from the expenses (`android/app/src/main/java/.../domain/`).

## Run it yourself

### 1. Set up the server (free)

1. Create a project at [supabase.com](https://supabase.com).
2. **Authentication → Sign In / Providers → Allow anonymous sign-ins**: turn it on.
3. **SQL Editor**: run the files in `supabase/migrations/` in order (`0001`, `0002`, `0003`).
   Or, with the [Supabase CLI](https://supabase.com/docs/guides/cli): `supabase link` then `supabase db push`.
4. **Project Settings → API**: copy the project URL and the `anon` public key.

Free Supabase projects pause after a week with no activity. Open the dashboard to resume one.

### 2. Build the app

Needs Android Studio (or JDK 17 + Android SDK 37).

Add your server to `android/local.properties` (gitignored):

```properties
SUPABASE_URL=https://YOUR-PROJECT.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

Then:

```sh
cd android
./gradlew installDebug        # build and install on a connected phone
./gradlew testDebugUnitTest   # run the unit tests
```

The anon key is meant to be public. It's safe in the app because the database rules protect the data.

## Invite links

The app shares links like `https://anand8290.github.io/splitlite/join/?c=CODE`. That page (`docs/join/`)
opens the app through `splitlite://join?c=CODE`. If it doesn't open, people can type the code in the app.
Set `JOIN_URL` in `local.properties` to point at your own copy of the page.

## Project layout

```
android/            Android app (single :app module)
  app/src/main/java/com/anand8290/splitlite/
    domain/         splitting, balances, money formatting — pure Kotlin, unit-tested
    data/           Supabase calls and the offline JSON cache
    ui/             Compose screens
supabase/migrations SQL schema, access rules and functions
docs/               GitHub Pages: privacy policy, data deletion, invite page
fastlane/metadata/  store listing text (Google Play and F-Droid)
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). The goal is a small app that does one thing well. Please open an
issue before adding a feature.

## License

[GPL-3.0](LICENSE)
