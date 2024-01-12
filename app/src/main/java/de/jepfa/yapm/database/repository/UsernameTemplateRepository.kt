package de.jepfa.yapm.database.repository

import androidx.annotation.WorkerThread
import de.jepfa.yapm.database.dao.EncUsernameTemplateDao
import de.jepfa.yapm.database.entity.EncUsernameTemplateEntity
import de.jepfa.yapm.model.encrypted.EncUsernameTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class UsernameTemplateRepository(private val encUsernameTemplateDao: EncUsernameTemplateDao) {

    @WorkerThread
    suspend fun insert(encUsernameTemplate: EncUsernameTemplate) {
        encUsernameTemplateDao.insert(mapToEntity(encUsernameTemplate))
    }

    @WorkerThread
    suspend fun update(encUsernameTemplate: EncUsernameTemplate) {
        encUsernameTemplateDao.update(mapToEntity(encUsernameTemplate))
    }

    @WorkerThread
    suspend fun delete(encUsernameTemplate: EncUsernameTemplate) {
        encUsernameTemplateDao.delete(mapToEntity(encUsernameTemplate))
    }

    @WorkerThread
    suspend fun deleteById(id: Int) {
        encUsernameTemplateDao.deleteById(id)
    }

    @WorkerThread
    suspend fun findByIdSync(id: Int): EncUsernameTemplate? {
        val entity = encUsernameTemplateDao.getByIdSync(id)
        if (entity != null) {
            return mapToModel(entity)
        }
        else {
            return null
        }
    }

    fun getById(id: Int): Flow<EncUsernameTemplate> {
        val model = encUsernameTemplateDao.getById(id)
        return model.filterNotNull().map { mapToModel(it)}
    }

    fun getAll(): Flow<List<EncUsernameTemplate>> {
        return encUsernameTemplateDao.getAll().filterNotNull().map { mapToModels(it)}
    }

    fun getAllSync(): List<EncUsernameTemplate> {
        return encUsernameTemplateDao.getAllSync().map { mapToModel(it)}
    }

    private fun mapToModels(entities: List<EncUsernameTemplateEntity>): List<EncUsernameTemplate> {
        return entities.map { mapToModel(it) }.toList()
    }

    private fun mapToModel(entity: EncUsernameTemplateEntity): EncUsernameTemplate {
        return EncUsernameTemplate(entity.id,
                entity.username,
                entity.description,
                entity.generatorType,
        )
    }

    private fun mapToEntity(encUsernameTemplate: EncUsernameTemplate): EncUsernameTemplateEntity {
        return EncUsernameTemplateEntity(
            encUsernameTemplate.id,
            encUsernameTemplate.username,
            encUsernameTemplate.description,
            encUsernameTemplate.generatorType,
        )
    }

}