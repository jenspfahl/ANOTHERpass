package de.jepfa.yapm.usecase.secret

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.encrypted.EncryptedType.Types.MASTER_PASSWD_TOKEN
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.usecase.BasicUseCase
import de.jepfa.yapm.util.putEncryptedExtra

object GenerateMasterPasswordTokenUseCase: BasicUseCase<SecureActivity>() {

    fun openDialog(activity: SecureActivity) {
        val mptCounter = PreferenceService.getAsInt(
            PreferenceService.STATE_MASTER_PASSWD_TOKEN_COUNTER,
            activity
        )
        if (PreferenceService.isPresent(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, activity)) {
            AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.title_generate_mpt))
                    .setMessage(activity.getString(R.string.message_generate_mpt, mptCounter))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                        UseCaseBackgroundLauncher(GenerateMasterPasswordTokenUseCase)
                            .launch(activity, Unit)
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()

        }
        else {
            UseCaseBackgroundLauncher(GenerateMasterPasswordTokenUseCase)
                .launch(activity, Unit)        }
    }

    override fun execute(activity: SecureActivity): Boolean {
        val key = activity.masterSecretKey
        val encMasterPasswd = Session.getEncMasterPasswd()
        if (key != null && encMasterPasswd != null) {
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
            val mptKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP_TOKEN)
            val masterPassword = SecretService.decryptPassword(tempKey, encMasterPasswd)

            val nextMptNumber = PreferenceService.getAsInt(
                PreferenceService.STATE_MASTER_PASSWD_TOKEN_COUNTER,
                activity
            ) + 1
            val cipherAlgorithm = SecretService.getCipherAlgorithm(activity)

            val masterPasswordTokenKey = SecretService.generateRandomKey(32)
            val encMasterPasswordTokenKey = SecretService.encryptKey(mptKey, masterPasswordTokenKey)
            val masterPasswordTokenSK = SecretService.generateStrongSecretKey(
                masterPasswordTokenKey,
                SaltService.getSalt(activity),
                cipherAlgorithm
            )
            val type = EncryptedType(MASTER_PASSWD_TOKEN, nextMptNumber.toString())
            val tokenizedMasterPassword =
                SecretService.encryptPassword(type, masterPasswordTokenSK, masterPassword)
            val encTokenizedMasterPassword =
                SecretService.encryptEncrypted(tempKey, tokenizedMasterPassword)

            val encHead = SecretService.encryptCommonString(
                tempKey,
                activity.getString(R.string.head_generate_mpt, nextMptNumber)
            )
            val encSub = SecretService.encryptCommonString(
                tempKey,
                activity.getString(R.string.sub_generate_mpt)
            )
            val encQrcHeader =
                SecretService.encryptCommonString(tempKey, encTokenizedMasterPassword.type?.toString() ?: "")
            val encQrc = encTokenizedMasterPassword

            PreferenceService.putEncrypted(
                PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
                encMasterPasswordTokenKey,
                activity
            )
            PreferenceService.putString(
                PreferenceService.STATE_MASTER_PASSWD_TOKEN_COUNTER,
                nextMptNumber.toString(),
                activity
            )

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, encHead)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, encQrcHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, encQrc)
            intent.putExtra(QrCodeActivity.EXTRA_COLOR, Color.BLUE)
            intent.putExtra(QrCodeActivity.EXTRA_NFC_WITH_APP_RECORD, true)

            activity.startActivity(intent)

            masterPassword.clear()
        }

        return true
    }

}