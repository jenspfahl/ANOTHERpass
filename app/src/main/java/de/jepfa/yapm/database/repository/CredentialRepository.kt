package de.jepfa.yapm.database.repository

import androidx.annotation.WorkerThread
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.model.encrypted.EncCredential
import kotlinx.coroutines.flow.*
import java.util.*

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

    @WorkerThread
    suspend fun findByIdSync(id: Int): EncCredential? {
        val entity = encCredentialDao.getByIdSync(id)
        if (entity != null) {
            return mapToCredential(entity)
        }
        else {
            return null
        }
    }

    fun getById(id: Int): Flow<EncCredential> {
        val byId = encCredentialDao.getById(id)
        return byId.filterNotNull().map {mapToCredential(it)}
    }

    fun findById(id: Int): Flow<EncCredential?> {
        val byId = encCredentialDao.getById(id)
        return byId.map { if (it != null) mapToCredential(it) else null }
    }

    fun findByUid(uid: UUID): Flow<EncCredential?> {
        val byUid = encCredentialDao.getByUid(uid.toString())
        return byUid.map { if (it != null) mapToCredential(it) else null }
    }

    fun getAll(): Flow<List<EncCredential>> {
        return encCredentialDao.getAll().filterNotNull().map {mapToCredentials(it)}
    }

    fun getAllSync(): List<EncCredential> {
        return encCredentialDao.getAllSync().map {mapToCredential(it)}
    }

    private fun mapToCredentials(entities: List<EncCredentialEntity>): List<EncCredential> {
        return entities.map {mapToCredential(it) }.toList()
    }

    private fun mapToCredential(entity: EncCredentialEntity): EncCredential {
        return EncCredential(entity.id,
                entity.uid,
                entity.name,
                entity.additionalInfo,
                entity.user,
                entity.password,
                entity.lastPassword,
                entity.website,
                entity.labels,
                entity.isObfuscated,
                entity.isLastPasswordObfuscated,
                entity.modifyTimestamp)
    }

    private fun mapToEntity(encCredential: EncCredential): EncCredentialEntity {
        return EncCredentialEntity(encCredential.id,
                encCredential.uid,
                encCredential.name,
                encCredential.additionalInfo,
                encCredential.user,
                encCredential.password,
                encCredential.lastPassword,
                encCredential.website,
                encCredential.labels,
                encCredential.isObfuscated,
                encCredential.isLastPasswordObfuscated,
                encCredential.modifyTimestamp ?: encCredential.id?.toLong() ?: 0)
    }

}