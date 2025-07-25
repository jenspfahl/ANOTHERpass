package de.jepfa.yapm.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import de.jepfa.yapm.database.repository.CredentialRepository
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_EXPIRY_DATES
import de.jepfa.yapm.service.io.AutoBackupService
import de.jepfa.yapm.service.notification.NotificationService
import de.jepfa.yapm.service.notification.NotificationService.SCHEDULED_NOTIFICATION_KEY_SEPARATOR
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.YapmApp
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.addDays
import de.jepfa.yapm.util.removeTime
import kotlinx.coroutines.launch
import java.util.*

class CredentialViewModel(private val repository: CredentialRepository) : ViewModel() {

    val allCredentials: LiveData<List<EncCredential>> = repository.getAll().asLiveData()
    private val credentialIdsAndExpiresAt = HashMap<Int, Long>() //Credential.id, GMT millis

    fun getById(id: Int): LiveData<EncCredential> {
        return repository.getById(id).asLiveData()
    }

    fun findById(id: Int): LiveData<EncCredential?> {
        return repository.findById(id).asLiveData()
    }

    fun findByUid(uid: UUID): LiveData<EncCredential?> {
        return repository.findByUid(uid).asLiveData()
    }

    fun insert(credential: EncCredential, context: Context) = viewModelScope.launch {
        credential.timeData.touchModify()
        repository.insert(credential)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }    

    fun update(credential: EncCredential, context: Context) = viewModelScope.launch {
        credential.timeData.touchModify()
        repository.update(credential)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }

    fun delete(credential: EncCredential, context: Context)  = viewModelScope.launch {
        credential.timeData.touchModify()
        repository.delete(credential)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }

    fun deleteAll(credentials: Collection<EncCredential>, context: Context)  = viewModelScope.launch {
        credentials.forEach { credential ->
            credential.timeData.touchModify()
            repository.delete(credential)
        }
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
        AutoBackupService.autoExportVault(context)
    }

    fun hasExpiredCredentials(): Boolean {
        val currMillis = System.currentTimeMillis()
        return credentialIdsAndExpiresAt.values.any { it < currMillis }
    }

    fun updateCredentialExpiry(credential: EncCredential, key: SecretKeyHolder, context: Context, considerExpiredForThePast: Boolean = false) {
        val id = credential.id
        if (id != null) {
            val currentMillis = if (considerExpiredForThePast) 0 else Date().removeTime().addDays(1).time
            val expiresAt = SecretService.decryptLong(key, credential.timeData.expiresAt)
            if (expiresAt != null && expiresAt > 0) {
                credentialIdsAndExpiresAt[id] = expiresAt
                Log.i(LOG_PREFIX + "NOTIF", "${credential.id}: ${Date(expiresAt)} >= ${Date(currentMillis)} ${expiresAt >= currentMillis}")
                if (expiresAt >= currentMillis) {
                    PreferenceService.putString(
                        DATA_EXPIRY_DATES + SCHEDULED_NOTIFICATION_KEY_SEPARATOR + id,
                        expiresAt.toString(),
                        null
                    )
                    NotificationService.scheduleNotification(context, id, Date(expiresAt))
                    Log.i(LOG_PREFIX + "NOTIF", "${credential.id}: scheduled for ${Date(expiresAt)}")

                }
            }
            else {
                deleteCredentialExpiry(id, context)
            }
        }
    }

    fun deleteCredentialExpiry(id: Int, context: Context) {
        val previous = credentialIdsAndExpiresAt.remove(id)
        if (previous != null) {
            Log.i(LOG_PREFIX + "EXP", "remove notification for $id")

            PreferenceService.delete(DATA_EXPIRY_DATES + SCHEDULED_NOTIFICATION_KEY_SEPARATOR + id, null)
            NotificationService.cancelScheduledNotification(context, id)
        }
    }


    fun clearCredentialExpiries() {
        credentialIdsAndExpiresAt.clear()
        PreferenceService.deleteAllStartingWith(DATA_EXPIRY_DATES, null)
    }
}


class CredentialViewModelFactory(private val app: YapmApp) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CredentialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CredentialViewModel(app.credentialRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}