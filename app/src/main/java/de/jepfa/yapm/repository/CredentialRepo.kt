package de.jepfa.yapm.repository

import androidx.annotation.WorkerThread
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.util.Base64Util

class CredentialRepo(private val encCredentialDao: EncCredentialDao) {

    @WorkerThread
    suspend fun insert(encCredential: EncCredential) {
        encCredentialDao.insert(mapToEntity(encCredential))
    }

    fun getById(id: Int): EncCredential? {
        val entity = encCredentialDao.getById(id)
        if (entity != null) {
            return mapToCredential(entity)
        }
        return null;
    }

    private fun mapToCredential(entity: EncCredentialEntity): EncCredential {
        return EncCredential(entity.id,
                Base64Util.stringToEncrypted(entity.name),
                Base64Util.stringToEncrypted(entity.additionalInfo),
                Base64Util.stringToEncrypted(entity.password),
                entity.extraPinRequired)
    }

    private fun mapToEntity(encCredential: EncCredential): EncCredentialEntity {
        return EncCredentialEntity(encCredential.id,
                Base64Util.encryptedToString(encCredential.name),
                Base64Util.encryptedToString(encCredential.additionalInfo),
                Base64Util.encryptedToString(encCredential.password),
                encCredential.extraPinRequired)
    }

}