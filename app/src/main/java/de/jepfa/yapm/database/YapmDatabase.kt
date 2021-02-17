package de.jepfa.yapm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import de.jepfa.yapm.database.dao.EncCredentialDao
import de.jepfa.yapm.database.entity.EncCredentialEntity
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.secretgenerator.PassphraseGenerator
import de.jepfa.yapm.service.secretgenerator.PassphraseGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
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

            val passphraseGenerator = PassphraseGenerator()
            val secretService = SecretService()
            val key = secretService.getAndroidSecretKey("test-key")

            val encName1 = secretService.encryptCommonString(key, "testname1")
            val encAdditionalInfo1 = secretService.encryptCommonString(key, "")
            val spec1 = PassphraseGeneratorSpec()
            val encPassword1 = secretService.encryptPassword(key, passphraseGenerator.generatePassphrase(spec1))
            var entity1 = EncCredentialEntity(null,
                    encName1,
                    encAdditionalInfo1,
                    encPassword1,
                    false)

            val encName2 = secretService.encryptCommonString(key, "testname2")
            val encAdditionalInfo2 = secretService.encryptCommonString(key, "hints")
            val spec2 = PassphraseGeneratorSpec(strength = PasswordStrength.SUPER_STRONG, addDigit = true)
            val encPassword2 = secretService.encryptPassword(key, passphraseGenerator.generatePassphrase(spec2))
            var entity2 = EncCredentialEntity(null,
                    encName2,
                    encAdditionalInfo2,
                    encPassword2,
                    false)

            val encName3 = secretService.encryptCommonString(key, "testname3")
            val encAdditionalInfo3 = secretService.encryptCommonString(key, "bla bla")
            val spec3 = PassphraseGeneratorSpec(wordBeginningUpperCase = true, addDigit = true, addSpecialChar = true)
            val encPassword3 = secretService.encryptPassword(key, passphraseGenerator.generatePassphrase(spec3))
            var entity3 = EncCredentialEntity(null,
                    encName3,
                    encAdditionalInfo3,
                    encPassword3,
                    true)

            credentialDao.insert(entity1)
            credentialDao.insert(entity2)
            credentialDao.insert(entity3)


        }
    }

}