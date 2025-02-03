package de.jepfa.yapm.database.repository

import androidx.annotation.WorkerThread
import de.jepfa.yapm.database.dao.EncLabelDao
import de.jepfa.yapm.database.entity.EncLabelEntity
import de.jepfa.yapm.model.encrypted.EncLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.*

class LabelRepository(private val encLabelDao: EncLabelDao) {

    @WorkerThread
    suspend fun insert(encLabel: EncLabel): EncLabel {
        val id = encLabelDao.insert(mapToEntity(encLabel))
        encLabel.id = id.toInt()
        return encLabel
    }

    @WorkerThread
    suspend fun update(encLabel: EncLabel) {
        encLabelDao.update(mapToEntity(encLabel))
    }

    @WorkerThread
    suspend fun delete(encLabel: EncLabel) {
        encLabelDao.delete(mapToEntity(encLabel))
    }

    @WorkerThread
    suspend fun deleteById(id: Int) {
        encLabelDao.deleteById(id)
    }

    @WorkerThread
    suspend fun deleteByIds(ids: List<Int>) {
        encLabelDao.deleteByIds(ids)
    }

    @WorkerThread
    suspend fun findByIdSync(id: Int): EncLabel? {
        val entity = encLabelDao.getByIdSync(id)
        if (entity != null) {
            return mapToLabel(entity)
        }
        else {
            return null
        }
    }

    fun getById(id: Int): Flow<EncLabel> {
        val encLabel = encLabelDao.getById(id)
        return encLabel.filterNotNull().map { mapToLabel(it)}
    }

    fun getAll(): Flow<List<EncLabel>> {
        return encLabelDao.getAll().filterNotNull().map { mapToLabels(it)}
    }

    fun getAllSync(): List<EncLabel> {
        return encLabelDao.getAllSync().map { mapToLabel(it)}
    }

    private fun mapToLabels(entities: List<EncLabelEntity>): List<EncLabel> {
        return entities.map { mapToLabel(it) }.toList()
    }

    private fun mapToLabel(entity: EncLabelEntity): EncLabel {
        return EncLabel(entity.id,
                entity.uid,
                entity.name,
                entity.description,
                entity.color)
    }

    private fun mapToEntity(encLabel: EncLabel): EncLabelEntity {
        return EncLabelEntity(encLabel.id,
                encLabel.uid ?: UUID.randomUUID(),
                encLabel.name,
                encLabel.description,
                encLabel.color)
    }

}