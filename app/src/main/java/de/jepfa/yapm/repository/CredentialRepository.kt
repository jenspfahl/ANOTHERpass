package de.jepfa.yapm.repository

import androidx.annotation.WorkerThread
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.util.Base64Util
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CredentialRepository(private val encCredentialDao: EncCredentialDao) {

    @WorkerThread
    suspend fun insert(encCredential: EncCredential) {
        encCredentialDao.insert(mapToEntity(encCredential))
    }

    @WorkerThread
    suspend fun update(encCredential: EncCredential) {
        encCredentialDao.update(mapToEntity(encCredential))
    }

    @WorkerThread
    suspend fun delete(encCredential: EncCredential) {
        encCredentialDao.delete(mapToEntity(encCredential))
    }

    fun getById(id: Int): EncCredential? {
        val entity = encCredentialDao.getById(id)
        if (entity != null) {
            return mapToCredential(entity)
        }
        return null;
    }

    fun getAll(): Flow<List<EncCredential>> {
        return encCredentialDao.getAll().map {it -> mapToCredentials(it)}
    }

    private fun mapToCredentials(entities: List<EncCredentialEntity>): List<EncCredential> {
        return entities.map { it -> mapToCredential(it) }.toList()
    }

    private fun mapToCredential(entity: EncCredentialEntity): EncCredential {
        return EncCredential(entity.id,
                Base64Util.base64StringToEncrypted(entity.name),
                Base64Util.base64StringToEncrypted(entity.additionalInfo),
                Base64Util.base64StringToEncrypted(entity.password),
                entity.extraPinRequired)
    }

    private fun mapToEntity(encCredential: EncCredential): EncCredentialEntity {
        return EncCredentialEntity(encCredential.id,
                Base64Util.encryptedToBase64String(encCredential.name),
                Base64Util.encryptedToBase64String(encCredential.additionalInfo),
                Base64Util.encryptedToBase64String(encCredential.password),
                encCredential.extraPinRequired)
    }

}