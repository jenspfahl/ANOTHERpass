package de.jepfa.yapm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.jepfa.yapm.database.converter.StringSetConverter
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.dao.EncLabelDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.database.entity.EncLabelEntity

const val DB_VERISON = 2

@Database(
    entities = [EncCredentialEntity::class, EncLabelEntity::class],
    version = DB_VERISON,
    exportSchema = false
)
@TypeConverters(StringSetConverter::class)
abstract class YapmDatabase : RoomDatabase() {
    abstract fun credentialDao(): EncCredentialDao
    abstract fun labelDao(): EncLabelDao

    companion object {

        fun getVersion(): Int {
            return DB_VERISON
        }

        @Volatile
        private var INSTANCE: YapmDatabase? = null
        fun getDatabase(context: Context): YapmDatabase? {
            if (INSTANCE == null) {
                synchronized(YapmDatabase::class.java) {
                    if (INSTANCE == null) {
                        val migration = object : Migration(1, 2) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("ALTER TABLE EncCredentialEntity ADD COLUMN lastPassword TEXT")
                            }
                        }

                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            YapmDatabase::class.java, "yapm_database"
                        )
                        .addMigrations(migration)
                        .build()
                    }
                }
            }
            return INSTANCE
        }
    }

}