package de.jepfa.yapm.service.io

import android.content.Context
import app.keemobile.kotpass.constants.BasicField
import app.keemobile.kotpass.cryptography.EncryptedValue
import app.keemobile.kotpass.database.Credentials
import app.keemobile.kotpass.database.KeePassDatabase
import app.keemobile.kotpass.database.decode
import app.keemobile.kotpass.database.encode
import app.keemobile.kotpass.database.modifiers.modifyParentGroup
import app.keemobile.kotpass.database.traverse
import app.keemobile.kotpass.models.Entry
import app.keemobile.kotpass.models.EntryFields
import app.keemobile.kotpass.models.EntryValue
import app.keemobile.kotpass.models.Meta
import app.keemobile.kotpass.models.TimeData
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.util.DebugInfo
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*


object KdbxService {

    private val credentialFactory: (String) -> Credentials = {
        Credentials.from(EncryptedValue.fromString(it)) //TODO use fromBinary
    }

    fun createKdbxExportContent(
        kdbxPassword: Password,
        credentials: List<EncCredential>,
        secretKey: SecretKeyHolder,
        outputStream: ByteArrayOutputStream,
        context: Context): Boolean {

        try {


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
                        val expiresAt = SecretService.decryptLong(secretKey, encCredential.timeData.expiresAt)
                        val modifiedAt = encCredential.timeData.modifyTimestamp
                        val user = SecretService.decryptCommonString(secretKey, encCredential.user)
                        val password = SecretService.decryptPassword(secretKey, encCredential.passwordData.password)
                        val additionalInfo = SecretService.decryptCommonString(secretKey, encCredential.additionalInfo)
                        val otpAuth = encCredential.otpData?.let {
                            SecretService.decryptCommonString(secretKey, it.encOtpAuthUri)
                        }

                        var fields = EntryFields.of(
                            BasicField.Title() to EntryValue.Plain(name),
                            BasicField.Url() to EntryValue.Plain(website),
                            BasicField.UserName() to EntryValue.Plain(user),
                            BasicField.Notes() to EntryValue.Plain(additionalInfo),
                            BasicField.Password() to EntryValue.Encrypted(
                                EncryptedValue.fromString(password.toRawFormattedPassword().toString())
                            ),
                        )

                        if (otpAuth != null) {
                            fields = fields.plus("otp" to EntryValue.Encrypted(
                                EncryptedValue.fromString(otpAuth)
                            ))
                        }

                        Entry(
                            encCredential.uid ?: UUID.randomUUID(),
                            fields = fields,
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
        } catch (e: Exception) {
            DebugInfo.logException("KDBX", "cannot export to KDBX format", e)
            return false
        }
    }

    fun readKdbxContent(
        kdbxPassword: Password,
        inputStream: InputStream,
    ): MutableList<CredentialFileRecord>? {
        try {
            val database = inputStream.use { KeePassDatabase.decode(it, credentialFactory(kdbxPassword.toString())) }

            val fileRecords = mutableListOf<CredentialFileRecord>()

            var count = 0
            database.traverse {
                when (it) {
                    is Entry -> {
                        val fields = it.fields
                        val times = it.times


                        if (fields.title != null && fields.password != null) {

                            var expiresAt = if (times?.expiryTime != null) Date.from(times.expiryTime) else null
                            // For some unknown reasons sometimes credentials from Keepass have an expiry date equal or before the creation date.
                            // If so, we clear the expiration date.
                            if (times?.creationTime != null && !times.creationTime!!.isBefore(times.expiryTime)) {
                                expiresAt = null
                            }

                            val modifiedAt = if (times?.lastModificationTime != null) Date.from(times.lastModificationTime) else null

                            val record = CredentialFileRecord(
                                it.uuid,
                                count,
                                fields.title?.content!!,
                                fields.url?.content,
                                fields.userName?.content,
                                fields.password?.content!!,
                                fields.notes?.content.orEmpty(),
                                if (expiresAt?.time == 0L) null else expiresAt,
                                if (modifiedAt?.time == 0L) null else modifiedAt,
                                it.tags,
                                fields["otp"]?.content
                            )

                            it.tags.forEach { tag ->
                                LabelService.externalHolder.updateLabel(Label(tag))
                            }

                            fileRecords.add(record)

                            count++
                        }


                    }

                    else -> {}
                }
            }
            return fileRecords
        } catch (e: Exception) {
            DebugInfo.logException("KDBX", "Cannot read file", e)
            return null
        }
    }
}