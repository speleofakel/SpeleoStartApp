package com.speleo.start.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("speleo_prefs", Context.MODE_PRIVATE)

    var activeCompetitionId: Long
        get() = prefs.getLong("active_competition_id", -1)
        set(value) = prefs.edit().putLong("active_competition_id", value).apply()

    val hasActiveCompetition: Boolean
        get() = activeCompetitionId != -1L
}