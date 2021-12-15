package de.jepfa.yapm.usecase.secret

import android.content.Intent
import android.graphics.Color
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.encrypted.EncryptedType.Types.ENC_MASTER_PASSWD
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseActivity
import de.jepfa.yapm.ui.nfc.NfcActivity
import de.jepfa.yapm.ui.qrcode.QrCodeActivity
import de.jepfa.yapm.usecase.InputUseCase
import de.jepfa.yapm.util.putEncryptedExtra

object ExportEncMasterPasswordUseCase:
    InputUseCase<ExportEncMasterPasswordUseCase.Input, BaseActivity>() {

    data class Input(val encMasterPasswd: Encrypted, val noSessionCheck: Boolean, val directlyToNfcActivity: Boolean = false)

    override fun doExecute(input: Input, activity: BaseActivity): Boolean {
        val tempKey = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, activity)

        val encHead =
            SecretService.encryptCommonString(tempKey, activity.getString(R.string.head_export_emp))
        val encSub =
            SecretService.encryptCommonString(tempKey, activity.getString(R.string.sub_export_emp))
        val masterPassword = SecretService.decryptPassword(tempKey, input.encMasterPasswd)
        val empSK = MasterPasswordService.generateEncMasterPasswdSKForExport(activity)
        val encMasterPasswd =
            SecretService.encryptPassword(EncryptedType(ENC_MASTER_PASSWD), empSK, masterPassword)
        val encQrcHeader = SecretService.encryptCommonString(tempKey, encMasterPasswd.type?.toString() ?: "")
        val encQrc = SecretService.encryptEncrypted(tempKey, encMasterPasswd)
        masterPassword.clear()

        PreferenceService.putCurrentDate(PreferenceService.DATA_MP_EXPORTED_AT, activity)

        if (input.directlyToNfcActivity) {
            val intent = Intent(activity, NfcActivity::class.java)
            intent.putExtra(NfcActivity.EXTRA_MODE, NfcActivity.EXTRA_MODE_RW)
            intent.putExtra(NfcActivity.EXTRA_WITH_APP_RECORD, true)
            intent.putExtra(NfcActivity.EXTRA_NO_SESSION_CHECK, input.noSessionCheck)
            intent.putEncryptedExtra(NfcActivity.EXTRA_DATA, encQrc)
            activity.startActivity(intent)
        }
        else {
            val intent = Intent(activity, QrCodeActivity::class.java)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_HEADLINE, encHead)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_SUBTEXT, encSub)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE_HEADER, encQrcHeader)
            intent.putEncryptedExtra(QrCodeActivity.EXTRA_QRCODE, encQrc)
            intent.putExtra(QrCodeActivity.EXTRA_COLOR, Color.RED)
            intent.putExtra(QrCodeActivity.EXTRA_NO_SESSION_CHECK, input.noSessionCheck)
            intent.putExtra(QrCodeActivity.EXTRA_NFC_WITH_APP_RECORD, true)
            activity.startActivity(intent)
        }

        return true
    }
}