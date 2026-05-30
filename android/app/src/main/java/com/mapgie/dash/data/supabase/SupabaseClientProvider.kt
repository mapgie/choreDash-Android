package com.mapgie.dash.data.supabase

import com.mapgie.dash.data.preferences.AppSettings
import com.mapgie.dash.data.preferences.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClientProvider @Inject constructor(
    settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _client = MutableStateFlow<SupabaseClient?>(null)
    val client: StateFlow<SupabaseClient?> = _client.asStateFlow()

    init {
        scope.launch {
            settingsRepository.settings.collect { settings ->
                _client.value?.close()
                _client.value = buildClient(settings)
            }
        }
    }

    private fun buildClient(settings: AppSettings): SupabaseClient? {
        if (settings.supabaseUrl.isBlank() || settings.supabaseKey.isBlank()) return null
        return createSupabaseClient(
            supabaseUrl = settings.supabaseUrl,
            supabaseKey = settings.supabaseKey
        ) {
            install(Postgrest)
        }
    }

    fun currentClient(): SupabaseClient? = _client.value
}
