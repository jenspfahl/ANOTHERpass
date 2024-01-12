package de.jepfa.yapm.database.dao

import androidx.room.*
import de.jepfa.yapm.database.entity.EncUsernameTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EncUsernameTemplateDao {
    @Insert
    suspend fun insert(encUsernameTemplateEntity: EncUsernameTemplateEntity)

    @Update
    suspend fun update(encUsernameTemplateEntity: EncUsernameTemplateEntity)

    @Delete
    suspend fun delete(encUsernameTemplateEntity: EncUsernameTemplateEntity)

    @Query("DELETE FROM EncUsernameTemplateEntity WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT * FROM EncUsernameTemplateEntity WHERE id = :id")
    fun getById(id: Int): Flow<EncUsernameTemplateEntity>

    @Query("SELECT * FROM EncUsernameTemplateEntity WHERE id = :id")
    suspend fun getByIdSync(id: Int): EncUsernameTemplateEntity?

    @Query("SELECT * FROM EncUsernameTemplateEntity")
    fun getAll(): Flow<List<EncUsernameTemplateEntity>>

    @Query("SELECT * FROM EncUsernameTemplateEntity ORDER BY id")
    fun getAllSync(): List<EncUsernameTemplateEntity>

}