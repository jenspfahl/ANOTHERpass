package de.jepfa.yapm.database.dao

import androidx.room.*
import de.jepfa.yapm.database.entity.EncWebExtensionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EncWebExtensionDao {
    @Insert
    suspend fun insert(encWebExtensionEntity: EncWebExtensionEntity)

    @Update
    suspend fun update(encWebExtensionEntity: EncWebExtensionEntity)

    @Delete
    suspend fun delete(encWebExtensionEntity: EncWebExtensionEntity)

    @Query("DELETE FROM EncWebExtensionEntity WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM EncWebExtensionEntity WHERE id = :id")
    fun getById(id: Int): Flow<EncWebExtensionEntity>

   @Query("SELECT * FROM EncWebExtensionEntity")
    fun getAll(): Flow<List<EncWebExtensionEntity>>

}