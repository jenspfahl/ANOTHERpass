package de.jepfa.yapm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.dao.EncLabelDao
import de.jepfa.yapm.database.dao.EncUsernameTemplateDao
import de.jepfa.yapm.database.dao.EncWebExtensionDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.database.entity.EncLabelEntity
import de.jepfa.yapm.database.entity.EncUsernameTemplateEntity
import de.jepfa.yapm.database.entity.EncWebExtensionEntity
import java.util.*

const val DB_VERSION = 8

@Database(
    entities = [EncCredentialEntity::class, EncLabelEntity::class, EncUsernameTemplateEntity::class, EncWebExtensionEntity::class],
    version = DB_VERSION,
    exportSchema = false
)
abstract class YapmDatabase : RoomDatabase() {
    abstract fun credentialDao(): EncCredentialDao
    abstract fun labelDao(): EncLabelDao
    abstract fun usernameTemplateDao(): EncUsernameTemplateDao
    abstract fun webExtensionDao(): EncWebExtensionDao

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

                                updateUuid(database, EncCredentialEntity::class.simpleName!!)
                                updateUuid(database, EncLabelEntity::class.simpleName!!)
                            }
                        }
                        val migration5to6 = object : Migration(5, 6) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("ALTER TABLE EncCredentialEntity ADD COLUMN expiresAt TEXT")

                                updateUuid(database, EncCredentialEntity::class.simpleName!!)
                                updateUuid(database, EncLabelEntity::class.simpleName!!)
                            }
                        }
                        val migration6to7 = object : Migration(6, 7) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("CREATE TABLE IF NOT EXISTS `EncUsernameTemplateEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `username` TEXT NOT NULL, `description` TEXT NOT NULL, `generatorType` TEXT NOT NULL)");
                            }
                        }
                        val migration7to8 = object : Migration(7, 8) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("CREATE TABLE IF NOT EXISTS `EncWebExtensionEntity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `webClientId` TEXT NOT NULL, `title` TEXT, `extensionPublicKeyAlias` TEXT, `serverKeyPairAlias` TEXT, `linked` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, `lastUsedTimestamp` INTEGER)");
                            }
                        }

                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            YapmDatabase::class.java, "yapm_database"
                        )
                        .addMigrations(
                            migration1to2,
                            migration2to3,
                            migration3to4,
                            migration4to5,
                            migration5to6,
                            migration6to7,
                            migration7to8
                        )
                        .build()
                    }
                }
            }
            return INSTANCE
        }

        private fun updateUuid(database: SupportSQLiteDatabase, entityName: String) {
            val cursor = database.query("SELECT id FROM $entityName where UID is null")
            try {
                if (cursor.count > 0) {
                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(
                            cursor.getColumnIndexOrThrow("id")
                        )
                        val uuid = UUID.randomUUID()
                        database.execSQL(
                            "UPDATE $entityName SET uid =:1 WHERE id=:2",
                            arrayOf(uuid.toString(), id.toLong())
                        )
                    }
                }
            } finally {
                cursor.close()
            }
        }
    }

}