package de.jepfa.yapm.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import de.jepfa.yapm.database.YapmDatabase
import de.jepfa.yapm.database.repository.CredentialRepository
import de.jepfa.yapm.database.repository.LabelRepository
import de.jepfa.yapm.service.PreferenceService

class YapmApp : Application() {

    val database by lazy { YapmDatabase.getDatabase(this) }
    val credentialRepository by lazy { CredentialRepository(database!!.credentialDao()) }
    val labelRepository by lazy { LabelRepository(database!!.labelDao()) }

    override fun onCreate() {
        super.onCreate()
        PreferenceService.initDefaults(this.applicationContext)
        val darkMode = PreferenceService.getAsInt(PreferenceService.PREF_DARK_MODE, this.applicationContext)
        AppCompatDelegate.setDefaultNightMode(darkMode)
    }
}
