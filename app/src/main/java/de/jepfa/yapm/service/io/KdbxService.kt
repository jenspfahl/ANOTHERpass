package de.jepfa.yapm.service.io

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import app.keemobile.kotpass.constants.BasicField
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.encode
import app.keemobile.kotpass.database.modifiers.modifyGroups
import app.keemobile.kotpass.database.modifiers.modifyParentGroup
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.EntryFields
import app.keemobile.kotpass.models.EntryValue
import app.keemobile.kotpass.models.Meta
import app.keemobile.kotpass.models.TimeData
import com.opencsv.CSVReaderHeaderAware
import com.opencsv.CSVWriter
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import de.jepfa.yapm.util.FileUtil
import de.jepfa.yapm.util.toSimpleDateFormat
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.StringReader
import java.time.Instant
import java.util.*


object KdbxService {

    fun createKdbxExportContent(
        kdbxPassword: Password,
        credentials: List<EncCredential>,
        secretKey: SecretKeyHolder,
        outputStream: ByteArrayOutputStream,
        context: Context): Boolean {

        try {

            val credentialFactory: (String) -> Credentials = {
                Credentials.from(EncryptedValue.fromString(it))
            }

            val meta = Meta(
                generator = "ANOTHERpass",
                name = "ANOTHERpass vault '${SaltService.getVaultId(context)}'",
                description = "Exported from an ANOTHERpass vault with Id ${SaltService.getVaultId(context)}",
            )
            val database = KeePassDatabase.Ver4x.create("ANOTHERpass", meta, credentialFactory(kdbxPassword.toRawFormattedPassword().toString())).run {

                modifyParentGroup {
                    copy(entries = credentials.map { encCredential ->

                        val name = SecretService.decryptCommonString(secretKey, encCredential.name)
                        val website = SecretService.decryptCommonString(secretKey, encCredential.website)
                        val expiresAt = SecretService.decryptLong(secretKey, encCredential.expiresAt)
                        val modifiedAt = encCredential.modifyTimestamp
                        val user = SecretService.decryptCommonString(secretKey, encCredential.user)
                        val password = SecretService.decryptPassword(secretKey, encCredential.password)
                        val additionalInfo = SecretService.decryptCommonString(secretKey, encCredential.additionalInfo)


                        Entry(
                            encCredential.uid ?: UUID.randomUUID(),
                            fields = EntryFields.of(
                                BasicField.Title() to EntryValue.Plain(name),
                                BasicField.Url() to EntryValue.Plain(website),
                                BasicField.UserName() to EntryValue.Plain(user),
                                BasicField.Notes() to EntryValue.Plain(additionalInfo),
                                BasicField.Password() to EntryValue.Encrypted(
                                    EncryptedValue.fromString(password.toRawFormattedPassword().toString())
                                ),
                            ),
                            times = TimeData(
                                lastModificationTime = if (modifiedAt != null && modifiedAt > 0) Date(modifiedAt).toInstant() else null,
                                expiryTime = if (expiresAt != null && expiresAt > 0) Date(expiresAt).toInstant() else null,
                                expires = (expiresAt != null && expiresAt > 0),
                                creationTime = null,
                                lastAccessTime = null,
                                locationChanged = null,
                            ),
                            tags = LabelService.defaultHolder.decryptLabelsForCredential(secretKey, encCredential)
                                .map { it.name }
                                .sorted()
                        )

                    })
                }
            }


            outputStream.use {
                database.encode(it)
            }

            return true
        } catch (e: IOException) {
            Log.e("KDBX", "cannot export to KDBX format", e)
            return false
        }
    }
}