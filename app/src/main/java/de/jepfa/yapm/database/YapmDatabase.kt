package de.jepfa.yapm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.model.EncCredential

@Database(
    entities = [EncCredential::class],
    version = 1,
    exportSchema = false
)
abstract class YapmDatabase : RoomDatabase() {
    abstract fun credentialDao(): EncCredentialDao

    companion object {
        @Volatile
        private var INSTANCE: YapmDatabase? = null
        fun getDatabase(context: Context): YapmDatabase? {
            if (INSTANCE == null) {
                synchronized(YapmDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            YapmDatabase::class.java, "yapm_database"
                        ).build()
                    }
                }
            }
            return INSTANCE
        }
    }
}