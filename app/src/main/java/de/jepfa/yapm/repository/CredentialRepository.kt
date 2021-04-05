package de.jepfa.yapm.repository

import androidx.annotation.WorkerThread
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.model.EncCredential
import kotlinx.coroutines.flow.*

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

    fun getById(id: Int): Flow<EncCredential> {
        val byId = encCredentialDao.getById(id)
        return byId.filterNotNull().map { it -> mapToCredential(it)}
    }

    fun getAll(): Flow<List<EncCredential>> {
        return encCredentialDao.getAll().filterNotNull().map {it -> mapToCredentials(it)}
    }

    fun getAllSync(): List<EncCredential> {
        return encCredentialDao.getAllSync().filterNotNull().map {it -> mapToCredential(it)}
    }

    private fun mapToCredentials(entities: List<EncCredentialEntity>): List<EncCredential> {
        return entities.map { it -> mapToCredential(it) }.toList()
    }

    private fun mapToCredential(entity: EncCredentialEntity): EncCredential {
        return EncCredential(entity.id,
                entity.name,
                entity.additionalInfo,
                entity.user,
                entity.password,
                entity.website,
                entity.labels)
    }

    private fun mapToEntity(encCredential: EncCredential): EncCredentialEntity {
        return EncCredentialEntity(encCredential.id,
                encCredential.name,
                encCredential.additionalInfo,
                encCredential.user,
                encCredential.password,
                encCredential.website,
                encCredential.labels)
    }

}