package de.jepfa.yapm.usecase

import android.content.Intent
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.putEncryptedExtra

object ExportCredentialUseCase {

    fun startExport(credential: EncCredential, activity: SecureActivity): Boolean {
        activity.masterSecretKey?.let{ key ->
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
            val credentialName = SecretService.decryptCommonString(
                key,
                credential.name
            )
            val tempEncHeader = SecretService.encryptCommonString(
                tempKey, "The plain password of '$credentialName'"
            )

            val tempEncSubtext = SecretService.encryptCommonString(
                tempKey, "Keep this QR code safe since it contains a real plain password!"
            )
            val tempEncName = SecretService.encryptCommonString(
                tempKey, credentialName
            )
            val tempEncPasswd = SecretService.encryptPassword(
                tempKey, SecretService.decryptPassword(
                    key,
                    credential.password
                )
            )

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, credential.id)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, tempEncHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, tempEncSubtext)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, tempEncPasswd)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, tempEncName)

            activity.startActivity(intent)
        }
        return true
    }

}