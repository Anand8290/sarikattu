# R8 is on for release builds (app/build.gradle.kts).
# No extra rules are needed today: kotlinx.serialization, Ktor and supabase-kt ship their own
# consumer rules. If a release build crashes where debug doesn't, add the keep rule here and
# test the release APK on a device before shipping.
