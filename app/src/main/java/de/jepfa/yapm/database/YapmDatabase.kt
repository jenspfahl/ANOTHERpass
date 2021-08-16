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

const val DB_VERSION = 3

@Database(
    entities = [EncCredentialEntity::class, EncLabelEntity::class],
    version = DB_VERSION,
    exportSchema = false
)
@TypeConverters(StringSetConverter::class)
abstract class YapmDatabase : RoomDatabase() {
    abstract fun credentialDao(): EncCredentialDao
    abstract fun labelDao(): EncLabelDao

    companion object {

        fun getVersion(): Int {
            return DB_VERSION
        }

        @Volatile
        private var INSTANCE: YapmDatabase? = null
        fun getDatabase(context: Context): YapmDatabase? {
            if (INSTANCE == null) {
                synchronized(YapmDatabase::class.java) {
                    if (INSTANCE == null) {
                        val migration1to2 = object : Migration(1, 2) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("ALTER TABLE EncCredentialEntity ADD COLUMN lastPassword TEXT")
                            }
                        }
                        val migration2to3 = object : Migration(2, 3) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("ALTER TABLE EncCredentialEntity ADD COLUMN isObfuscated INTEGER NOT NULL DEFAULT 0")
                            }
                        }

                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            YapmDatabase::class.java, "yapm_database"
                        )
                        .addMigrations(migration1to2, migration2to3)
                        .build()
                    }
                }
            }
            return INSTANCE
        }
    }

}