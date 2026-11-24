package com.nononsensestudios.sarikattu

import android.app.Application
import android.content.Context
import com.nononsensestudios.sarikattu.data.LocalCache
import com.nononsensestudios.sarikattu.data.SplitRepository
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

class SariKattuApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
