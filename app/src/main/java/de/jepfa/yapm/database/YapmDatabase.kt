package de.jepfa.yapm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.dao.EncLabelDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.database.entity.EncLabelEntity
import java.util.*

const val DB_VERSION = 5

@Database(
    entities = [EncCredentialEntity::class, EncLabelEntity::class],
    version = DB_VERSION,
    exportSchema = false
)
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
                                database.execSQL("ALTER TABLE EncCredentialEntity ADD COLUMN isLastPasswordObfuscated INTEGER NOT NULL DEFAULT 0")
                            }
                        }
                        val migration3to4 = object : Migration(3, 4) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("ALTER TABLE EncCredentialEntity ADD COLUMN modifyTimestamp INTEGER NOT NULL DEFAULT 0")
                                database.execSQL("UPDATE EncCredentialEntity SET modifyTimestamp=id")
                            }
                        }
                        val migration4to5 = object : Migration(4, 5) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("ALTER TABLE EncCredentialEntity ADD COLUMN uid TEXT")
                                database.execSQL("ALTER TABLE EncLabelEntity ADD COLUMN uid TEXT")
                                database.execSQL("CREATE UNIQUE INDEX index_EncCredentialEntity_uid ON EncCredentialEntity(uid)")
                                database.execSQL("CREATE UNIQUE INDEX index_EncLabelEntity_uid ON EncLabelEntity(uid)")

                                val credentialsCursor =
                                    database.query("SELECT id FROM EncCredentialEntity")
                                database.beginTransaction()
                                try {
                                    if (credentialsCursor.count > 0) {
                                        while (credentialsCursor.moveToNext()) {
                                            val id = credentialsCursor.getInt(
                                                credentialsCursor.getColumnIndexOrThrow("id")
                                            )
                                            val uuid = UUID.randomUUID()
                                            database.execSQL(
                                                "UPDATE EncCredentialEntity SET uid =:1 WHERE id=:2",
                                                arrayOf(uuid.toString(), id.toLong())
                                            )
                                        }
                                        database.setTransactionSuccessful()
                                    }
                                } finally {
                                    database.endTransaction()
                                }
                                credentialsCursor.close()

                                val labelsCursor = database.query("SELECT id FROM EncLabelEntity")
                                database.beginTransaction()
                                try {
                                    if (labelsCursor.count > 0) {
                                        while (labelsCursor.moveToNext()) {
                                            val id = labelsCursor.getInt(
                                                labelsCursor.getColumnIndexOrThrow("id")
                                            )
                                            val uuid = UUID.randomUUID()
                                            database.execSQL(
                                                "UPDATE EncLabelEntity SET uid =:1 WHERE id=:2",
                                                arrayOf(uuid.toString(), id.toLong())
                                            )
                                        }
                                        database.setTransactionSuccessful()
                                    }
                                } finally {
                                    labelsCursor.close()
                                    database.endTransaction()
                                }
                            }
                        }

                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            YapmDatabase::class.java, "yapm_database"
                        )
                        .addMigrations(migration1to2, migration2to3, migration3to4, migration4to5)
                        .build()
                    }
                }
            }
            return INSTANCE
        }
    }

}