package de.jepfa.yapm.database.repository

import androidx.annotation.WorkerThread
import de.jepfa.yapm.database.dao.EncWebExtensionDao
import de.jepfa.yapm.database.entity.EncWebExtensionEntity
import de.jepfa.yapm.model.encrypted.EncWebExtension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class WebExtensionRepository(private val encWebExtensionDao: EncWebExtensionDao) {

    @WorkerThread
    suspend fun insert(encWebExtension: EncWebExtension) {
        encWebExtensionDao.insert(mapToEntity(encWebExtension))
    }

    @WorkerThread
    suspend fun update(encWebExtension: EncWebExtension) {
        encWebExtensionDao.update(mapToEntity(encWebExtension))
    }


    @WorkerThread
    suspend fun save(encWebExtension: EncWebExtension) {
        if (encWebExtension.isPersistent()) {
            update(encWebExtension)
        }
        else {
            insert(encWebExtension)
        }
    }


    @WorkerThread
    suspend fun delete(encWebExtension: EncWebExtension) {
        encWebExtensionDao.delete(mapToEntity(encWebExtension))
    }

    @WorkerThread
    suspend fun deleteById(id: Int) {
        encWebExtensionDao.deleteById(id)
    }

    fun getById(id: Int): Flow<EncWebExtension> {
        val model = encWebExtensionDao.getById(id)
        return model.filterNotNull().map { mapToModel(it)}
    }

    fun getAll(): Flow<List<EncWebExtension>> {
        return encWebExtensionDao.getAll().filterNotNull().map { mapToModels(it)}
    }


    private fun mapToModels(entities: List<EncWebExtensionEntity>): List<EncWebExtension> {
        return entities.map { mapToModel(it) }.toList()
    }

    private fun mapToModel(entity: EncWebExtensionEntity): EncWebExtension {
        return EncWebExtension(entity.id,
            entity.webClientId,
            entity.title,
            entity.extensionPublicKeyAlias,
            entity.serverKeyPairAlias,
            entity.linked,
            entity.enabled,
            entity.lastUsedTimestamp,
        )
    }

    private fun mapToEntity(encWebExtension: EncWebExtension): EncWebExtensionEntity {
        return EncWebExtensionEntity(
            encWebExtension.id,
            encWebExtension.webClientId,
            encWebExtension.title,
            encWebExtension.extensionPublicKey,
            encWebExtension.serverDomainName,
            encWebExtension.linked,
            encWebExtension.enabled,
            encWebExtension.lastUsedTimestamp,
        )
    }

}