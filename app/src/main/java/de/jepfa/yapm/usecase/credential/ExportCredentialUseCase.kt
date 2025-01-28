package de.jepfa.yapm.usecase.credential

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.export.DecryptedExportableCredential
import de.jepfa.yapm.model.export.PlainShareableCredential
import de.jepfa.yapm.model.otp.OtpConfig
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.io.VaultExportService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.decryptLong
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.putEncryptedExtra
import de.jepfa.yapm.util.toBase64String

object ExportCredentialUseCase: InputUseCase<ExportCredentialUseCase.Input, SecureActivity>() {

    enum class ExportMode(val labelId: Int, val colorId: Int? = null) {
        PLAIN_PASSWD(R.string.export_plain_passwd, Color.RED),
        PLAIN_CREDENTIAL_RECORD(R.string.export_plain_credential_record, Color.RED),
        ENC_CREDENTIAL_RECORD(R.string.export_enc_credential_record),
    }

    data class Input(val mode: ExportMode, val credential: EncCredential, val obfuscationKey: Key?)

    fun openStartExportDialog(credential: EncCredential, obfuscationKey: Key?, activity: SecureActivity) {
        val listItems = ExportMode.values().map { activity.getString(it.labelId) }.toTypedArray()

        AlertDialog.Builder(activity)
            .setIcon(R.drawable.ic_baseline_qr_code_24)
            .setTitle(R.string.export_credential)
            .setSingleChoiceItems(listItems, -1) { dialogInterface, i ->
                val mode = ExportMode.values()[i]
                UseCaseBackgroundLauncher(this)
                    .launch(activity, Input(mode, credential, obfuscationKey))
                dialogInterface.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    override suspend fun doExecute(input: Input, activity: SecureActivity): Boolean {
        activity.masterSecretKey?.let{ key ->
            val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, activity)
            val credentialName = SecretService.decryptCommonString(
                key,
                input.credential.name
            )
            val tempEncHeader = SecretService.encryptCommonString(
                tempKey, getHeaderDesc(input.mode, activity, credentialName)
            )

            val tempEncSubtext = SecretService.encryptCommonString(
                tempKey, getSubDesc(input.mode, activity)
            )
            val tempEncName = SecretService.encryptCommonString(
                tempKey, getQrCodeHeader(input.mode, credentialName)
            )

            val tempEncQrCode = getQrCodeData(input.mode, input.credential, tempKey, key, input.obfuscationKey)

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, input.credential.id)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, tempEncHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, tempEncSubtext)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, tempEncQrCode)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, tempEncName)
            intent.putExtra(QrCodeActivity.EXTRA_COLOR, input.mode.colorId)

            // will be forwarded to NfcActivity
            intent.putExtra(NfcActivity.EXTRA_WITH_APP_RECORD, true)

            activity.startActivity(intent)

            return true
        }

        return false
    }

    private fun getQrCodeHeader(
        mode: ExportMode,
        credentialName: String
    ) :String {
        return when (mode) {
            ExportMode.PLAIN_PASSWD -> credentialName
            ExportMode.ENC_CREDENTIAL_RECORD -> "$credentialName (ECR)"
            ExportMode.PLAIN_CREDENTIAL_RECORD -> "$credentialName (PCR)"
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
                val encForTransportPasswd = SecretService.encryptPassword(transportKey, passwd)
                passwd.clear()
                return encForTransportPasswd
            }
            ExportMode.ENC_CREDENTIAL_RECORD -> {
                val passwd = decryptPasswd(credential, key, obfuscationKey)
                val expiresAt = decryptLong(key, credential.timeData.expiresAt)
                val otpAuth = credential.otpData?.let {
                    SecretService.decryptCommonString(key, it.encOtpAuthUri)
                }

                val decryptedExportableCredential = DecryptedExportableCredential(
                    credential.id!!,
                    credential.uid?.toBase64String(),
                    SecretService.decryptCommonString(key, credential.name),
                    SecretService.decryptCommonString(key, credential.additionalInfo),
                    SecretService.decryptCommonString(key, credential.user),
                    passwd,
                    SecretService.decryptCommonString(key, credential.website),
                    SecretService.decryptCommonString(key, credential.labels),
                    if (expiresAt != null && expiresAt > 0) expiresAt else null,
                    if (otpAuth != null) OtpConfig.packOtpAuthUri(otpAuth) else null,
                    credential.passwordData.isObfuscated,
                    credential.timeData.modifyTimestamp,
                )
                val jsonString = VaultExportService.credentialToJson(decryptedExportableCredential)
                val encCredential = SecretService.encryptCommonString(key, jsonString)
                val encJsonString = VaultExportService.encExportableCredentialToJson(encCredential)
                val encForTransportData = SecretService.encryptCommonString(transportKey, encJsonString)
                passwd.clear()
                return encForTransportData
            }
            ExportMode.PLAIN_CREDENTIAL_RECORD -> {
                val passwd = decryptPasswd(credential, key, obfuscationKey)
                val expiresAt = decryptLong(key, credential.timeData.expiresAt)
                val otpAuth = credential.otpData?.let {
                    SecretService.decryptCommonString(key, it.encOtpAuthUri)
                }

                val shortPlainCredential = PlainShareableCredential(
                    credential.uid?.toBase64String(),
                    SecretService.decryptCommonString(key, credential.name),
                    SecretService.decryptCommonString(key, credential.additionalInfo),
                    SecretService.decryptCommonString(key, credential.user),
                    passwd,
                    SecretService.decryptCommonString(key, credential.website),
                    if (expiresAt != null && expiresAt > 0) expiresAt else null,
                    if (otpAuth != null) OtpConfig.packOtpAuthUri(otpAuth) else null
                )
                val jsonString = VaultExportService.credentialToJson(shortPlainCredential)
                val encForTransportData = SecretService.encryptCommonString(transportKey, jsonString)
                passwd.clear()
                return encForTransportData
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
            credential.passwordData.password
        )
        obfuscationKey?.let {
            passwd.deobfuscate(it)
        }
        return passwd
    }

}