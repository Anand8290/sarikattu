package com.anand8290.splitlite

import android.app.Application
import android.content.Context
import com.anand8290.splitlite.data.LocalCache
import com.anand8290.splitlite.data.SplitRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import java.io.File

/** Hand-written dependency container: the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val isConfigured: Boolean = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private val supabase = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL.ifBlank { "https://not-configured.invalid" },
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY.ifBlank { "not-configured" },
    ) {
        install(Auth)
        install(Postgrest)
    }

    val repository = SplitRepository(supabase, LocalCache(File(context.filesDir, "cache")))
}

class SplitLiteApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
