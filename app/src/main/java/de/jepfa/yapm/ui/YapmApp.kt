package de.jepfa.yapm.ui

import android.app.Application
import de.jepfa.yapm.database.YapmDatabase
import de.jepfa.yapm.repository.CredentialRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class YapmApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { YapmDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { CredentialRepository(database!!.credentialDao()) }
}
