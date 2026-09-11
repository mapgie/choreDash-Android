package com.mapgie.dash.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tagStickerDataStore: DataStore<Preferences> by preferencesDataStore(name = "dash_tag_stickers")

/**
 * Which tag ids this phone has met on a physical NFC sticker, and when: an id
 * it wrote to a sticker, or one it read off a sticker (a chore tap, a tag-alarm
 * tap, a scan to link or identify). Every chore has a tag id in Supabase whether
 * or not a sticker exists, so this is the only evidence the app has that one
 * does. Kept on-device and never synced: it is this phone's experience, and
 * Settings › NFC tags uses it to split chores into "on a sticker" and "no
 * sticker".
 */
@Singleton
class TagStickerStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SEEN = stringPreferencesKey("seen")
    }

    private val json = Json { ignoreUnknownKeys = true }

    /** Tag id to the last time it was written to or read from a sticker. */
    val seen: Flow<Map<String, Instant>> = context.tagStickerDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[Keys.SEEN]) }

    /** Records that [tagId] was just written to, or read from, a sticker. */
    suspend fun record(tagId: String, at: Instant = Instant.now()) {
        if (tagId.isBlank()) return
        context.tagStickerDataStore.edit { prefs ->
            val current = decode(prefs[Keys.SEEN]).mapValues { it.value.toString() }
            prefs[Keys.SEEN] = json.encodeToString(current + (tagId to at.toString()))
        }
    }

    suspend fun current(): Map<String, Instant> = seen.first()

    private fun decode(raw: String?): Map<String, Instant> =
        raw?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
            .orEmpty()
            .mapNotNull { (id, at) -> runCatching { Instant.parse(at) }.getOrNull()?.let { id to it } }
            .toMap()
}
