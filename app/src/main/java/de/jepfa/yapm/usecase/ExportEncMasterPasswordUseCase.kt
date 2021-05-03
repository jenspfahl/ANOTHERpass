package de.jepfa.yapm.usecase

import android.content.Intent
import android.graphics.Color
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.util.putEncryptedExtra

object ExportEncMasterPasswordUseCase {

    fun execute(encMasterPasswd: Encrypted, noSessionCheck: Boolean, activity: BaseActivity): Boolean {

        val tempKey = SecretService.getAndroidSecretKey(SecretService.ALIAS_KEY_TRANSPORT)

        val encHead = SecretService.encryptCommonString(tempKey, "Your Encrypted Master Password")
        val encSub = SecretService.encryptCommonString(tempKey, "Store this on a safe place since this is contains your master password.")
        val masterPassword = SecretService.decryptPassword(tempKey, encMasterPasswd)
        val salt = SecretService.getSalt(activity)
        val saltSK = SecretService.generateFastSecretKey(salt, salt)
        val encMasterPasswd = SecretService.encryptPassword(Encrypted.TYPE_ENC_MASTER_PASSWD, saltSK, masterPassword)
        val encQrcHeader = SecretService.encryptCommonString(tempKey, encMasterPasswd.type)
        val encQrc = SecretService.encryptEncrypted(tempKey, encMasterPasswd)
        masterPassword.clear()

        val intent = Intent(activity, QrCodeActivity::class.java)
        intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, encHead)
        intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub)
        intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, encQrcHeader)
        intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, encQrc)
        intent.putExtra(QrCodeActivity.EXTRA_COLOR, Color.RED)
        intent.putExtra(QrCodeActivity.EXTRA_NO_SESSION_CHECK, noSessionCheck)
        activity.startActivity(intent)

        return true
    }
}