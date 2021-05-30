package de.jepfa.yapm.usecase

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.DATA_MASTER_PASSWORD_TOKEN_KEY
import de.jepfa.yapm.util.PreferenceUtil.STATE_MASTER_PASSWD_TOKEN_COUNTER
import de.jepfa.yapm.util.putEncryptedExtra

object GenerateMasterPasswordTokenUseCase: SecureActivityUseCase {

    override fun execute(activity: SecureActivity): Boolean {
        val mptCounter = PreferenceUtil.getAsInt(STATE_MASTER_PASSWD_TOKEN_COUNTER, 0, activity)
        if (PreferenceUtil.isPresent(DATA_MASTER_PASSWORD_TOKEN_KEY, activity)) {
            AlertDialog.Builder(activity)
                    .setTitle("Generate master password token")
                    .setMessage("The last generated token with number #$mptCounter will be become invalid.")
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
            val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)
            val mPTKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_MP_TOKEN)
            val masterPassword = SecretService.decryptPassword(tempKey, encMasterPasswd)

            val masterPasswordTokenKey = SecretService.generateKey(32)
            val encMasterPasswordTokenKey = SecretService.encryptKey(mPTKey, masterPasswordTokenKey)
            val masterPasswordTokenSK = SecretService.generateSecretKey(masterPasswordTokenKey, SaltService.getSalt(activity))
            val masterPasswordToken = SecretService.encryptPassword(Encrypted.TYPE_MASTER_PASSWD_TOKEN, masterPasswordTokenSK, masterPassword)
            val encMasterPasswordToken = SecretService.encryptEncrypted(tempKey, masterPasswordToken)

            var nextMptNumber = PreferenceUtil.getAsInt(STATE_MASTER_PASSWD_TOKEN_COUNTER, 0, activity) + 1

            val encHead = SecretService.encryptCommonString(tempKey, "Your Master Password Token #$nextMptNumber")
            val encSub = SecretService.encryptCommonString(tempKey, "Take this token in your wallet to scan for login. If you loose it, just create a new one.")
            val encQrcHeader = SecretService.encryptCommonString(tempKey, "${encMasterPasswordToken.type} #$nextMptNumber")
            val encQrc = encMasterPasswordToken

            PreferenceUtil.putEncrypted(DATA_MASTER_PASSWORD_TOKEN_KEY, encMasterPasswordTokenKey, activity)
            PreferenceUtil.put(STATE_MASTER_PASSWD_TOKEN_COUNTER, nextMptNumber.toString(), activity)


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
    }

}