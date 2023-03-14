package de.jepfa.yapm.ui.login

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.NavHostFragment
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.Encrypted
import de.jepfa.yapm.model.encrypted.EncryptedType
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_MAX_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.PreferenceService.PREF_SELF_DESTRUCTION
import de.jepfa.yapm.service.PreferenceService.STATE_INTRO_SHOWED
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_DENIED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_SUCCEEDED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_PREVIOUS_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.PreferenceService.STATE_PREVIOUS_LOGIN_SUCCEEDED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_WHATS_NEW_SHOWED_FOR_VERSION
import de.jepfa.yapm.service.autofill.ResponseFiller
import de.jepfa.yapm.service.nfc.NfcService
import de.jepfa.yapm.service.secret.*
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.ui.credential.DeobfuscationDialog
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.ui.importvault.ImportVaultActivity
import de.jepfa.yapm.ui.intro.IntroActivity
import de.jepfa.yapm.ui.intro.WhatsNewActivity
import de.jepfa.yapm.ui.nfc.NfcBaseActivity
import de.jepfa.yapm.usecase.app.ShowInfoUseCase
import de.jepfa.yapm.usecase.vault.DropVaultUseCase
import de.jepfa.yapm.util.Constants
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.toastText


class LoginActivity : NfcBaseActivity() {

    var loginAttempts = 0
    var showTagDetectedMessage = false
    var isFromAutofill = false

    val createVaultActivityRequestCode = 1
    val importVaultActivityRequestCode = 2

    private var resumeAutofillItem: MenuItem? = null

    init {
        checkSession = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        isFromAutofill = intent.getBooleanExtra(SecretChecker.fromAutofillOrNotification, false)
        loginAttempts = PreferenceService.getAsInt(STATE_LOGIN_ATTEMPTS,  this)

        super.onCreate(null)

        if (!isFromAutofill && Session.isLoggedOut()) {
            val introShowed = PreferenceService.getAsBool(STATE_INTRO_SHOWED, this)
            if (!introShowed) {
                val intent = Intent(this, IntroActivity::class.java)
                startActivity(intent)
                // don't show WhatsNew when Intro was just showed automatically
                PreferenceService.putInt(STATE_WHATS_NEW_SHOWED_FOR_VERSION,
                    DebugInfo.getVersionCodeForWhatsNew(this), this)

            }
            else {
                val whatsNewShowedForVersion = PreferenceService.getAsInt(STATE_WHATS_NEW_SHOWED_FOR_VERSION, this)
                val currentVersion = DebugInfo.getVersionCodeForWhatsNew(this)
                if (currentVersion > whatsNewShowedForVersion) {
                    val intent = Intent(this, WhatsNewActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        if (MasterKeyService.isMasterKeyStored(this)) {
            setContentView(R.layout.activity_login)
            nfcAdapter = NfcService.getNfcAdapter(this)
            readTagFromIntent(intent)

        }
        else {
            setContentView(R.layout.activity_create_or_import_vault)
            val buttonCreateVault: Button = findViewById(R.id.button_create_vault)
            buttonCreateVault.setOnClickListener {

                if (MasterKeyService.isMasterKeyStored(this)) {
                    toastText(this, R.string.vault_already_created)
                    return@setOnClickListener
                }

                val intent = Intent(this@LoginActivity, CreateVaultActivity::class.java)
                startActivityForResult(intent, importVaultActivityRequestCode)
            }
            val buttonImportVault: Button = findViewById(R.id.button_import_vault)
            buttonImportVault.setOnClickListener {
                if (MasterKeyService.isMasterKeyStored(this)) {
                    toastText(this, R.string.vault_already_created)
                    return@setOnClickListener
                }

                val intent = Intent(this@LoginActivity, ImportVaultActivity::class.java)
                startActivityForResult(intent, createVaultActivityRequestCode)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        val debugItem: MenuItem = menu.findItem(R.id.menu_debug)
        debugItem.isVisible = DebugInfo.isDebug

        resumeAutofillItem = menu.findItem(R.id.menu_resume_autofill)
        updateResumeAutfillMenuItem()

        return true
    }

    override fun onResume() {
        super.onResume()
        updateResumeAutfillMenuItem()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_login_help) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Constants.HOMEPAGE)
            startActivity(browserIntent)
            return true
        }

        if (id == R.id.menu_intro) {
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            return true
        }

        if (id == R.id.menu_whats_new) {
            val intent = Intent(this, WhatsNewActivity::class.java)
            startActivity(intent)
            return true
        }

        if (id == R.id.action_reset_vault) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_reset_all))
                .setMessage(getString(R.string.message_reset_all))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.reset_vault) { dialog, whichButton ->
                    DropVaultUseCase.doubleCheckDropVault(this)
                        {dropAndLogoutVault()}
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
            return true
        }

        if (id == R.id.menu_resume_autofill) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ResponseFiller.isAutofillPaused(this)) {
                ResponseFiller.resumeAutofill(this)
                toastText(this, R.string.resume_paused_autofill_done)
                item.isVisible = false
            }
            return true
        }

        if (id == R.id.menu_about) {
            ShowInfoUseCase.execute(this)
            return true
        }
        if (id == R.id.menu_debug) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            val icon: Drawable = applicationInfo.loadIcon(packageManager)
            val message = DebugInfo.getDebugInfo(this)
            builder.setTitle(R.string.debug)
                .setMessage(message)
                .setIcon(icon)
                .show()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == createVaultActivityRequestCode && resultCode == Activity.RESULT_OK) {
            recreate()
        }
        if (requestCode == importVaultActivityRequestCode && resultCode == Activity.RESULT_OK) {
            recreate()
        }
    }

    fun handleFailedLoginAttempt() {
        loginAttempts++

        PreferenceService.putInt(STATE_LOGIN_ATTEMPTS, loginAttempts, this)
        PreferenceService.putInt(STATE_PREVIOUS_LOGIN_ATTEMPTS, loginAttempts, this)

        PreferenceService.putCurrentDate(STATE_LOGIN_DENIED_AT, this)

        if (loginAttempts >= getMaxLoginAttempts()) {
            val selfDestruction = PreferenceService.getAsBool(PREF_SELF_DESTRUCTION, this)

            if (selfDestruction) {
                toastText(baseContext, R.string.too_may_wrong_logins_self_destruction)
                DropVaultUseCase.dropVaultData(this)
            }
            else {
                toastText(baseContext, R.string.too_may_wrong_logins)
                MasterPasswordService.deleteStoredMasterPassword(baseContext)
                PreferenceService.delete(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY, baseContext)
            }
            Session.logout()
            finishAffinity()
            finishAndRemoveTask()
        }
    }

    override fun handleTag() {
        Log.i("LOGIN", "tag detected " + ndefTag?.tagId)
        if (ndefTag != null && showTagDetectedMessage) {
            toastText(this, R.string.nfc_tag_for_login_detected)
        }

        val navFragment = supportFragmentManager.primaryNavigationFragment
        if (navFragment != null && navFragment is NavHostFragment) {
            val currFragment = navFragment.childFragmentManager.primaryNavigationFragment
            if (currFragment is LoginEnterMasterPasswordFragment) {
                currFragment.updateMasterKeyFromNfcTag()
            }
        }
    }

    override fun lock() {

    }

    fun getLoginAttemptMessage(): String {
        return getString(R.string.login_attempt, loginAttempts, getMaxLoginAttempts())
    }

    fun loginSuccessful() {
        loginAttempts = 0

        // backup last succeeded login
        PreferenceService.getAsDate(STATE_LOGIN_SUCCEEDED_AT, this)?.let {
            PreferenceService.putDate(STATE_PREVIOUS_LOGIN_SUCCEEDED_AT, it, this)
        }

        PreferenceService.putCurrentDate(STATE_LOGIN_SUCCEEDED_AT, this)
        PreferenceService.delete(STATE_LOGIN_ATTEMPTS, this)

        val isFromAutofill = intent.getBooleanExtra(SecretChecker.fromAutofillOrNotification, false)
        if (isFromAutofill) {
            setResult(SecretChecker.loginRequestCode, intent)
        }
        else {
            val intent = Intent(this, ListCredentialsActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    internal fun readMasterPassword(scanned: String, isFromQRScan: Boolean, handlePassword: (passwd: Password?) -> Unit) {
        val encrypted = Encrypted.fromEncryptedBase64StringWithCheck(scanned)
        when (encrypted?.type?.type) {
            EncryptedType.Types.ENC_MASTER_PASSWD -> readEMP(encrypted, handlePassword)
            EncryptedType.Types.MASTER_PASSWD_TOKEN -> readMPT(encrypted, isFromQRScan, handlePassword)
            else -> {
                toastText(this, R.string.invalid_emp_mpt)
            }
        }
    }

    internal fun isFastLoginWithQrCode(): Boolean {
        return  PreferenceService.getAsBool(PreferenceService.PREF_FAST_MASTERPASSWD_LOGIN_WITH_QRC, this)
    }

    internal fun isFastLoginWithNfcTag(): Boolean {
        return  PreferenceService.getAsBool(PreferenceService.PREF_FAST_MASTERPASSWD_LOGIN_WITH_NFC, this)
    }

    private fun updateResumeAutfillMenuItem() {
        resumeAutofillItem?.isVisible =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && ResponseFiller.isAutofillPaused(this)
    }

    private fun readEMP(emp: Encrypted, handlePassword: (passwd: Password?) -> Unit) {
        val empSK = MasterPasswordService.generateEncMasterPasswdSKForExport(this)
        val masterPassword = SecretService.decryptPassword(empSK, emp)
        if (!masterPassword.isValid()) {
            toastText(this, R.string.invalid_emp)
        }
        else if (MasterPasswordService.isProtectedEMP(emp)) {
            DeobfuscationDialog.openDeobfuscationDialogForMasterPassword(this) { deobfuscationKey ->
                if (deobfuscationKey != null) {
                    masterPassword.deobfuscate(deobfuscationKey)
                    handlePassword(masterPassword)
                }
                else {
                    handlePassword(null)
                }
            }
        }
        else {
            handlePassword(masterPassword)
        }

    }

    private fun readMPT(mpt: Encrypted, isFromQRScan: Boolean, handlePassword: (passwd: Password) -> Unit) {
        if (!PreferenceService.isPresent(
                PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
                this
            )
        ) {
            toastText(this, R.string.no_mpt_present)
            return
        }
        val tagId = ndefTag?.tagId
        val storedTagId = PreferenceService.getAsString(PreferenceService.DATA_MASTER_PASSWORD_TOKEN_NFC_TAG_ID, this)
        if (storedTagId != null) {
            if (isFromQRScan) {
                Log.i("nfc", "mpt qr code scan not allowed for copy-protected nfc tokens")
                toastText(this, R.string.mpt_qrcode_scan_not_allowed)
                return
            }
            else if (tagId == null) {
                toastText(this, "This NFC tag doesn't have an Id but is needed to verify it.")
                return
            }
            else if (tagId != storedTagId) {
                Log.i("nfc", "mpt tag id missmatch: tagId = $tagId <> storedTagId=$storedTagId")
                toastText(this, R.string.not_a_original_mpt_nfc_token)

                return
            }
        }

        // decrypt obliviously encrypted master password token
        val encMasterPasswordTokenKey = PreferenceService.getEncrypted(
            PreferenceService.DATA_MASTER_PASSWORD_TOKEN_KEY,
            this
        )
        encMasterPasswordTokenKey?.let {
            val masterPasswordTokenSK =
                SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_MP_TOKEN, this)
            val masterPasswordTokenKey =
                SecretService.decryptKey(masterPasswordTokenSK, encMasterPasswordTokenKey)
            val salt = SaltService.getSalt(this)
            val cipherAlgorithm = SecretService.getCipherAlgorithm(this)

            val mptSK =
                SecretService.generateDefaultSecretKey(masterPasswordTokenKey, salt, cipherAlgorithm)

            val masterPassword =
                SecretService.decryptPassword(mptSK, mpt)
            if (masterPassword.isValid()) {
                handlePassword(masterPassword)
                return
            }

        }
        toastText(this, R.string.invalid_mpt)
    }

    private fun getMaxLoginAttempts(): Int {
        return PreferenceService.getAsInt(PREF_MAX_LOGIN_ATTEMPTS, this)
    }


    private fun dropAndLogoutVault() {
        DropVaultUseCase.dropVaultData(this)
        Session.logout()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}