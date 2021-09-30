package de.jepfa.yapm.usecase

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.export.EncExportableCredential
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.export.PlainShareableCredential
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.io.FileIOService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.putEncryptedExtra

object ExportCredentialUseCase {

    enum class ExportMode(val labelId: Int, val colorId: Int? = null) {
        PLAIN_PASSWD(R.string.export_plain_passwd, Color.RED),
        PLAIN_CREDENTIAL_RECORD(R.string.export_plain_credential_record, Color.RED),
        ENC_CREDENTIAL_RECORD(R.string.export_enc_credential_record),
    }

    fun openStartExportDialog(credential: EncCredential, obfuscationKey: Key?, activity: SecureActivity) {
        val listItems = ExportMode.values().map { activity.getString(it.labelId) }.toTypedArray()

        AlertDialog.Builder(activity)
            .setTitle(R.string.export_credential)
            .setSingleChoiceItems(listItems, -1) { dialogInterface, i ->
                val mode = ExportMode.values()[i]
                startExport(mode, credential, obfuscationKey, activity)
                dialogInterface.dismiss()
            }
            .setNeutralButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    fun startExport(mode: ExportMode, credential: EncCredential, obfuscationKey: Key?, activity: SecureActivity) {
        activity.masterSecretKey?.let{ key ->
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
            val credentialName = SecretService.decryptCommonString(
                key,
                credential.name
            )
            val tempEncHeader = SecretService.encryptCommonString(
                tempKey, getHeaderDesc(mode, activity, credentialName)
            )

            val tempEncSubtext = SecretService.encryptCommonString(
                tempKey, getSubDesc(mode, activity)
            )
            val tempEncName = SecretService.encryptCommonString(
                tempKey, credentialName
            )

            val tempEncQrCode = getQrCodeData(mode, credential, tempKey, key, obfuscationKey)

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, credential.id)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, tempEncHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, tempEncSubtext)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, tempEncQrCode)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, tempEncName)
            intent.putExtra(QrCodeActivity.EXTRA_COLOR, mode.colorId)


            activity.startActivity(intent)
        }
    }

    private fun getHeaderDesc(
        mode: ExportMode,
        activity: SecureActivity,
        credentialName: String
    ) :String {
        return when (mode) {
            ExportMode.PLAIN_PASSWD -> activity.getString(R.string.head_export_plain_passwd, credentialName)
            ExportMode.ENC_CREDENTIAL_RECORD -> activity.getString(R.string.head_export_enc_credential_record, credentialName)
            ExportMode.PLAIN_CREDENTIAL_RECORD -> activity.getString(R.string.head_export_plain_credential_record, credentialName)
        }
    }

    private fun getSubDesc(
        mode: ExportMode,
        activity: SecureActivity
    ) :String {
        return when (mode) {
            ExportMode.PLAIN_PASSWD -> activity.getString(R.string.sub_export_plain_passwd)
            ExportMode.ENC_CREDENTIAL_RECORD -> activity.getString(R.string.sub_export_enc_credential_record)
            ExportMode.PLAIN_CREDENTIAL_RECORD -> activity.getString(R.string.sub_export_plain_credential_record)
        }
    }

    private fun getQrCodeData(
        mode: ExportMode,
        credential: EncCredential,
        transportKey: SecretKeyHolder,
        key: SecretKeyHolder,
        obfuscationKey: Key?
    ): Encrypted {
        return when (mode) {
            ExportMode.PLAIN_PASSWD -> {
                val passwd = decryptPasswd(credential, key, obfuscationKey)
                val encPasswd = SecretService.encryptPassword(transportKey, passwd)
                passwd.clear()
                return encPasswd
            }
            ExportMode.ENC_CREDENTIAL_RECORD -> {
                val shortEncCredential = EncExportableCredential(credential)
                val jsonString = FileIOService.exportCredential(shortEncCredential)
                return SecretService.encryptCommonString(transportKey, jsonString)
            }
            ExportMode.PLAIN_CREDENTIAL_RECORD -> {
                val passwd = decryptPasswd(credential, key, obfuscationKey)

                val shortPlainCredential = PlainShareableCredential(
                    SecretService.decryptCommonString(key, credential.name),
                    SecretService.decryptCommonString(key, credential.additionalInfo),
                    SecretService.decryptCommonString(key, credential.user),
                    passwd,
                    SecretService.decryptCommonString(key, credential.website)
                )
                val jsonString = FileIOService.exportCredential(shortPlainCredential)
                val encData = SecretService.encryptCommonString(transportKey, jsonString)
                passwd.clear()
                return encData
            }
        }

    }

    private fun decryptPasswd(
        credential: EncCredential,
        key: SecretKeyHolder,
        obfuscationKey: Key?
    ): Password {
        val passwd = SecretService.decryptPassword(
            key,
            credential.password
        )
        obfuscationKey?.let {
            passwd.deobfuscate(it)
        }
        return passwd
    }

}