package com.mapgie.dash.data.model

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Unsaved Edit sheet drafts keyed by item id (or [NEW_DRAFT_KEY]), mirrored
 * into a [SavedStateHandle] as one JSON string under [key] so they survive
 * rotation and process death for as long as the session does. Owned by the
 * list ViewModels; the sheets never apply a stored draft on their own, they
 * offer it (see the design handoff, "Sheet dismissal & unsaved changes").
 */
class DraftStore<T>(
    private val handle: SavedStateHandle,
    private val key: String,
    valueSerializer: KSerializer<T>,
) {
    private val serializer = MapSerializer(String.serializer(), valueSerializer)
    private val _drafts = MutableStateFlow(decode(handle.get<String>(key)))

    /** Every draft currently held, keyed by item id. */
    val drafts: StateFlow<Map<String, T>> = _drafts.asStateFlow()

    fun get(id: String): T? = _drafts.value[id]

    fun put(id: String, draft: T) {
        if (_drafts.value[id] == draft) return
        write(_drafts.value + (id to draft))
    }

    fun clear(id: String) {
        if (id !in _drafts.value) return
        write(_drafts.value - id)
    }

    private fun write(next: Map<String, T>) {
        _drafts.value = next
        handle[key] = json.encodeToString(serializer, next)
    }

    private fun decode(raw: String?): Map<String, T> =
        raw?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyMap()

    private companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
