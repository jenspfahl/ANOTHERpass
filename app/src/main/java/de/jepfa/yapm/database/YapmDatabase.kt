package de.jepfa.yapm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.util.Base64Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [EncCredentialEntity::class],
    version = 1,
    exportSchema = false
)
abstract class YapmDatabase : RoomDatabase() {
    abstract fun credentialDao(): EncCredentialDao

    companion object {
        @Volatile
        private var INSTANCE: YapmDatabase? = null
        fun getDatabase(context: Context, scope: CoroutineScope): YapmDatabase? {
            if (INSTANCE == null) {
                synchronized(YapmDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(
                            context.applicationContext,
                            YapmDatabase::class.java, "yapm_database"
                        )
                        .addCallback(YapmDatabaseCallback(scope))
                        .build()
                    }
                }
            }
            return INSTANCE
        }
    }

    private class YapmDatabaseCallback(
            private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(database: YapmDatabase) {
            // Delete all content here.
            database.clearAllTables()

            val credentialDao = database.credentialDao()

            val secretService = SecretService()
            val key = secretService.getAndroidSecretKey("test-key")

            val encName = secretService.encryptCommonString(key, "testname")
            val encAdditionalInfo = secretService.encryptCommonString(key, "")
            val encPassword = secretService.encryptPassword(key, Password("1234"))
            var entity = EncCredentialEntity(null,
                    Base64Util.encryptedToBase64String(encName),
                    Base64Util.encryptedToBase64String(encAdditionalInfo),
                    Base64Util.encryptedToBase64String(encPassword),
                    false)
            credentialDao.insert(entity)

        }
    }

}