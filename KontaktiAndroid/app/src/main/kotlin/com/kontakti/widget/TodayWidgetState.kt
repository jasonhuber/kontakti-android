package com.kontakti.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import com.google.gson.Gson
import com.kontakti.data.model.TodayItem
import kotlinx.coroutines.flow.first

private val Context.todayWidgetDataStore: DataStore<Preferences> by preferencesDataStore(name = "today_widget_state")

/**
 * Shared DataStore + helpers for the home-screen widget.
 *
 * The main app writes a compact JSON snapshot of the top reach-out items here
 * whenever Today data refreshes. The widget reads from the same store. We trigger
 * a re-render via [TodayWidget.updateAll] after each write.
 */
object TodayWidgetState {
    val TOP_ITEMS_JSON = stringPreferencesKey("top_items_json")
    val COUNT_TOTAL = intPreferencesKey("count_total")
    val UPDATED_AT = stringPreferencesKey("updated_at")

    /** Slim DTO so the widget doesn't have to deserialize the full Person. */
    data class WidgetItem(
        val id: String,
        val name: String,
        val reason: String,
        val avatarUrl: String? = null,
        val kind: String? = null
    )

    private val gson = Gson()

    suspend fun update(context: Context, items: List<TodayItem>, total: Int) {
        // Defensive: server filters DNC out of /today already, but never surface
        // a do-not-contact person on the home screen if a stale snapshot includes one.
        val safe = items.filterNot { it.person.doNotContact }
        val slim = safe.take(3).map {
            WidgetItem(
                id = it.person.id,
                name = it.person.fullName,
                reason = it.reason ?: defaultReasonFor(it.kind.name),
                avatarUrl = it.person.avatarUrl,
                kind = it.kind.name
            )
        }
        context.todayWidgetDataStore.edit { prefs ->
            prefs[TOP_ITEMS_JSON] = gson.toJson(slim)
            prefs[COUNT_TOTAL] = total
            prefs[UPDATED_AT] = System.currentTimeMillis().toString()
        }
        // Trigger Glance re-render.
        runCatching { TodayWidget().updateAll(context) }
    }

    suspend fun read(context: Context): Pair<List<WidgetItem>, Int> {
        val prefs = context.todayWidgetDataStore.data.first()
        val json = prefs[TOP_ITEMS_JSON].orEmpty()
        val total = prefs[COUNT_TOTAL] ?: 0
        val items = if (json.isBlank()) emptyList() else {
            runCatching {
                gson.fromJson(json, Array<WidgetItem>::class.java).toList()
            }.getOrDefault(emptyList())
        }
        return items to total
    }

    private fun defaultReasonFor(kind: String): String = when (kind) {
        "birthday" -> "Birthday today"
        "cadence_overdue" -> "Overdue for a check-in"
        "follow_up_due" -> "Follow-up due"
        "job_change" -> "Recent job change"
        "social_signal" -> "New activity"
        "anniversary_met" -> "Anniversary"
        "rhythm_broken" -> "Rhythm has slipped"
        else -> "Reach out"
    }
}
