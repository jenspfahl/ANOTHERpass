package de.jepfa.yapm.database.dao

import androidx.room.*
import de.jepfa.yapm.database.entity.EncLabelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EncLabelDao {
    @Insert
    suspend fun insert(encLabel: EncLabelEntity)

    @Update
    suspend fun update(encLabel: EncLabelEntity)

    @Delete
    suspend fun delete(encLabel: EncLabelEntity)

    @Query("DELETE FROM EncLabelEntity WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM EncLabelEntity WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT * FROM EncLabelEntity WHERE id = :id")
    fun getById(id: Int): Flow<EncLabelEntity>

    @Query("SELECT * FROM EncLabelEntity WHERE name = :encName")
    fun getByEncName(encName: String): Flow<EncLabelEntity>

    @Query("SELECT * FROM EncLabelEntity")
    fun getAll(): Flow<List<EncLabelEntity>>

    @Query("SELECT * FROM EncLabelEntity ORDER BY id")
    fun getAllSync(): List<EncLabelEntity>

}