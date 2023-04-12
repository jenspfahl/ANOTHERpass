package de.jepfa.yapm.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.database.repository.CredentialRepository
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.DATA_EXPIRY_DATES
import de.jepfa.yapm.service.notification.NotificationService
import de.jepfa.yapm.service.notification.NotificationService.SCHEDULED_NOTIFICATION_KEY_SEPARATOR
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.YapmApp
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
        credential.touchModify()
        repository.insert(credential)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
    }    

    fun update(credential: EncCredential, context: Context) = viewModelScope.launch {
        credential.touchModify()
        repository.update(credential)
        PreferenceService.putCurrentDate(PreferenceService.DATA_VAULT_MODIFIED_AT, context)
    }

    fun delete(credential: EncCredential)  = viewModelScope.launch {
        credential.touchModify()
        repository.delete(credential)
    }

    fun hasExpiredCredentials(): Boolean {
        val currMillis = System.currentTimeMillis()
        return credentialIdsAndExpiresAt.values.any { it < currMillis }
    }

    fun updateExpiredCredential(credential: EncCredential, key: SecretKeyHolder, context: Context, considerExpiredForThePast: Boolean = false) {
        val id = credential.id
        if (id != null) {
            val currentMillis = if (considerExpiredForThePast) 0 else Date().removeTime().addDays(1).time
            val expiresAt = SecretService.decryptLong(key, credential.expiresAt)
            if (expiresAt != null && expiresAt > 0) {
                credentialIdsAndExpiresAt[id] = expiresAt
                Log.i("NOTIF", "${credential.id}: ${Date(expiresAt)} >= ${Date(currentMillis)} ${expiresAt >= currentMillis}")
                if (expiresAt >= currentMillis) {
                    PreferenceService.putString(
                        DATA_EXPIRY_DATES + SCHEDULED_NOTIFICATION_KEY_SEPARATOR + id,
                        expiresAt.toString(),
                        null
                    )
                    NotificationService.scheduleNotification(context, id, Date(expiresAt))
                    Log.i("NOTIF", "${credential.id}: scheduled for ${Date(expiresAt)}")

                }
            }
            else {
                deleteExpiredCredential(id, context)
            }
        }
    }

    fun deleteExpiredCredential(id: Int, context: Context) {
        Log.i("EXP", "remove notification for $id")
        credentialIdsAndExpiresAt.remove(id)
        PreferenceService.delete(DATA_EXPIRY_DATES + SCHEDULED_NOTIFICATION_KEY_SEPARATOR + id, null)
        NotificationService.cancelScheduledNotification(context, id)
    }


    fun clearExpiredCredentials() {
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