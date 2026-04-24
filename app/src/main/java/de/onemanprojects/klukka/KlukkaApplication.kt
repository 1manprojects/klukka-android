package de.onemanprojects.klukka

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class KlukkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = AppPreferences(this)
        AppCompatDelegate.setDefaultNightMode(
            when (prefs.themeMode) {
                AppPreferences.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                AppPreferences.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
