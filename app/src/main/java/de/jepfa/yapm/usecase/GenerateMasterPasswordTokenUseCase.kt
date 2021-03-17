package de.jepfa.yapm.usecase

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.PreferenceUtil

object GenerateMasterPasswordTokenUseCase: UseCase {

    const val PREFIX = "!!!MPT!!!"

    override fun execute(activity: SecureActivity): Boolean {
        if (PreferenceUtil.isPresent(PreferenceUtil.PREF_MASTER_PASSWORD_TOKEN_KEY, activity)) {
            AlertDialog.Builder(activity)
                    .setTitle("Generate master password token")
                    .setMessage("All former generated tokens will be become invalid.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                        generateMasterPasswordToken(activity)
                    }
                    .setNegativeButton(android.R.string.no, null)
                    .show()

            return true
        }
        else {
            generateMasterPasswordToken(activity)
        }
        return true
    }

    private fun generateMasterPasswordToken(activity: SecureActivity) {
        val key = activity.masterSecretKey
        val encMasterPasswd = Session.getEncMasterPasswd()
        if (key != null && encMasterPasswd != null) {
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TEMP)
            val mPTKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP_TOKEN)
            val masterPassword = SecretService.decryptPassword(tempKey, encMasterPasswd)

            val masterPasswordTokenKey = SecretService.generateKey(32)
            val encMasterPasswordTokenKey = SecretService.encryptKey(mPTKey, masterPasswordTokenKey)
            val masterPasswordTokenSK = SecretService.generateSecretKey(masterPasswordTokenKey, SecretService.getOrCreateSalt(activity))
            val masterPasswordToken = SecretService.encryptPassword(masterPasswordTokenSK, masterPassword)
            val encMasterPasswordToken = SecretService.encryptEncrypted(tempKey, masterPasswordToken)

            val encHead = SecretService.encryptCommonString(tempKey, "Your master password token")
            val encSub = SecretService.encryptCommonString(tempKey, "Take this token in your wallet to scan for login. If you loose it, just create a new one.")
            val encQrc = encMasterPasswordToken

            PreferenceUtil.put(PreferenceUtil.PREF_MASTER_PASSWORD_TOKEN_KEY, encMasterPasswordTokenKey.toBase64String(), activity)

            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putExtra(QrCodeActivity.EXTRA_HEADLINE, encHead.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub.toBase64String())
            intent.putExtra(QrCodeActivity.EXTRA_QRCODE, typeString(encQrc.toBase64String()))
            intent.putExtra(QrCodeActivity.EXTRA_COLOR, Color.BLUE)

            activity.startActivity(intent)

            masterPassword.clear()
        }
    }

    private fun typeString(string: String): String {
        return ExportEncMasterKeyUseCase.PREFIX + string
    }

}