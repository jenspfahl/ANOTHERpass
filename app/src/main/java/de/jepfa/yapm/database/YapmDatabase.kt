package de.jepfa.yapm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.jepfa.yapm.database.converter.StringSetConverter
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.entity.EncCredentialEntity

@Database(
    entities = [EncCredentialEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringSetConverter::class)
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
                        )
                        .build()
                    }
                }
            }
            return INSTANCE
        }
    }

}