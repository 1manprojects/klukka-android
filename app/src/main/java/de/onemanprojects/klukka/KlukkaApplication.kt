package de.onemanprojects.klukka

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class KlukkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = AppPreferences(this)
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
