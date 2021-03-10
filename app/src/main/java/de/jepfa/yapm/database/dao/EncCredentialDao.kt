package de.jepfa.yapm.database.dao

import androidx.room.*
import de.jepfa.yapm.database.entity.EncCredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EncCredentialDao {
    @Insert
    suspend fun insert(encCredential: EncCredentialEntity)

    @Update
    suspend fun update(encCredential: EncCredentialEntity)

    @Delete
    suspend fun delete(encCredential: EncCredentialEntity)

    @Query("SELECT * FROM EncCredentialEntity WHERE id = :id")
    fun getById(id: Int): Flow<EncCredentialEntity>

    @Query("SELECT * FROM EncCredentialEntity")
    fun getAll(): Flow<List<EncCredentialEntity>>

    @Query("SELECT * FROM EncCredentialEntity ORDER BY id")
    fun getAllSync(): List<EncCredentialEntity>

}