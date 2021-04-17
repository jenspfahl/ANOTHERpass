package de.jepfa.yapm.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.asLiveData
import de.jepfa.yapm.database.dao.EncLabelDao
import de.jepfa.yapm.database.entity.EncLabelEntity
import de.jepfa.yapm.model.EncLabel
import de.jepfa.yapm.model.Encrypted
import kotlinx.coroutines.flow.*

class LabelRepository(private val encLabelDao: EncLabelDao) {

    @WorkerThread
    suspend fun insert(encLabel: EncLabel) {
        encLabelDao.insert(mapToEntity(encLabel))
    }

    @WorkerThread
    suspend fun update(encLabel: EncLabel) {
        encLabelDao.update(mapToEntity(encLabel))
    }

    @WorkerThread
    suspend fun delete(encLabel: EncLabel) {
        encLabelDao.delete(mapToEntity(encLabel))
    }

    fun getById(id: Int): Flow<EncLabel> {
        val encLabel = encLabelDao.getById(id)
        return encLabel.filterNotNull().map { mapToLabel(it)}
    }

    fun getAll(): Flow<List<EncLabel>> {
        return encLabelDao.getAll().filterNotNull().map { mapToLabels(it)}
    }

    fun getAllSync(): List<EncLabel> {
        return encLabelDao.getAllSync().filterNotNull().map { mapToLabel(it)}
    }

    private fun mapToLabels(entities: List<EncLabelEntity>): List<EncLabel> {
        return entities.map { mapToLabel(it) }.toList()
    }

    private fun mapToLabel(entity: EncLabelEntity): EncLabel {
        return EncLabel(entity.id,
                entity.name,
                entity.description,
                entity.color)
    }

    private fun mapToEntity(encLabel: EncLabel): EncLabelEntity {
        return EncLabelEntity(encLabel.id,
                encLabel.name,
                encLabel.description,
                encLabel.color)
    }

}